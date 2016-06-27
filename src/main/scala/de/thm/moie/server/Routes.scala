/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.{ActorRef, PoisonPill}
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import de.thm.moie.Global
import de.thm.moie.compiler.CompilerError
import de.thm.moie.project.{ProjectDescription, FilePath}
import de.thm.moie.server.ProjectManagerActor.{CompileProject, CompileScript, CompileDefaultScript}
import de.thm.moie.server.ProjectsManagerActor.{Disconnect, ProjectId, RemainingClients}

import java.nio.file.Paths
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait Routes extends JsonSupport {
  this: ServerSetup =>

  def projectsManager: ActorRef

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

  private def withIdExists[T : ToEntityMarshaller](id:Int)(fn: ActorRef => Future[T]) = {
      val projectsManagerOpt =  (projectsManager ? ProjectId(id)).mapTo[Option[ActorRef]]
      val future = projectsManagerOpt.collect {
        case Some(ref) => ref
      }.flatMap(fn)
      onComplete(future) {
        case Success(x) => complete(x)
        case Failure(_:NoSuchElementException) =>
          complete(StatusCodes.NotFound, s"unknown project-id $id")
        case Failure(NotFoundException(msg)) =>
          complete(StatusCodes.NotFound, msg)
        case Failure(t) =>
          serverlog.error(s"While using project $id msg: ${t.getMessage}")
          complete(StatusCodes.InternalServerError)
        }
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
        }
      } ~
      path("stop-server") {
        post {
          projectsManager ! PoisonPill
          shutdown("received `stop-server`")
          complete(StatusCodes.Accepted)
        }
      } ~
      pathPrefix("project" / IntNumber) { id =>
        path("disconnect") {
          post {
            disconnectWithExit(id)
            complete(StatusCodes.NoContent)
          }
        } ~
        path("compile") {
          get {
            withIdExists(id) { projectManager =>
              for {
                errors <- (projectManager ? CompileProject).mapTo[Seq[CompilerError]]
              } yield errors.toList
            }
          }
        } ~
        path("compileScript") {
          post {
            entity(as[FilePath]) { filepath =>
              withIdExists(id) { projectManager =>
                for {
                  errors <- (projectManager ? CompileScript(Paths.get(filepath.path))).mapTo[Seq[CompilerError]]
                } yield errors.toList
              }
            } ~
            withIdExists(id) { projectManager =>
              for {
                errors <- (projectManager ? CompileDefaultScript).mapTo[Seq[CompilerError]]
              } yield errors.toList
            }
          }
        }
      }
    }
}
