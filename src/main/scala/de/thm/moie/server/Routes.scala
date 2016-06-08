/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.pattern.ask
import akka.actor.{ActorRef, PoisonPill}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import de.thm.moie.Global
import de.thm.moie.compiler.CompilerError
import de.thm.moie.project.ProjectDescription
import de.thm.moie.project.ProjectDescription._
import de.thm.moie.server.ProjectManagerActor.CompileProject
import de.thm.moie.server.ProjectsManagerActor.{Disconnect, ProjectId, RemainingClients}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{Success, Failure}

trait Routes extends JsonSupport {
  this: ServerSetup =>

  def projectsManager: ActorRef

  private val withId = parameters("project-id".as[Int])
  private val exitOnLastDisconnect =
    Global.config.getBoolean("exitOnLastDisconnect").getOrElse(false)

  private def shutdown(cause:String="unkown cause"): Unit = {
    actorSystem.terminate()
    serverlog.info(s"Shutdown because $cause")
  }

  private def disconnectWithExit(id:Int):Unit =
    (projectsManager ? Disconnect(id)).
      mapTo[Option[RemainingClients]].flatMap {
        case Some(RemainingClients(0)) if exitOnLastDisconnect =>
          Future.successful(())
        case _ => Future.failed(new Exception())
      }.foreach { _ =>
        shutdown("no active clients left")
      }

  def routes =
    pathPrefix("moie") {
      path("connect") {
        post {
          entity(as[ProjectDescription]) { description =>
            complete {
              for {
                id <- (projectsManager ? description).mapTo[ProjectId]
              } yield IntJsonFormat.write(id.id)
            }
          }
        } ~ get {
          complete(ProjectDescription("Dummy-URL", "target", List()))
        }
      } ~
      path("disconnect") {
        withId { id =>
          disconnectWithExit(id)
          complete(StatusCodes.NoContent)
        }
      } ~
      path("stop-server") {
        projectsManager ! PoisonPill
        shutdown("received `stop-server`")
        complete(StatusCodes.Accepted)
      } ~
      path("compile") {
        withId { id =>
          //TODO refactor uising Future.collect
          val fut = for {
              projectManagerOpt <- (projectsManager ? ProjectId(id)).mapTo[Option[ActorRef]]
              if projectManagerOpt.isDefined
              errors <- (projectManagerOpt.get ? CompileProject).mapTo[Seq[CompilerError]]
            } yield errors.toList
          onComplete(fut) {
          case Success(lst) => complete(lst)
          case Failure(t) =>
            serverlog.error(s"While compiling project $id msg: ${t.getMessage}")
            complete(StatusCodes.NotFound)
          }
        }
      }
    }
}
