/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.pattern.ask
import akka.actor.{ActorRef, PoisonPill}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import de.thm.moie.compiler.CompilerError
import de.thm.moie.project.ProjectDescription
import de.thm.moie.project.ProjectDescription._
import de.thm.moie.server.ProjectManagerActor.CompileProject
import de.thm.moie.server.ProjectsManagerActor.{Disconnect, ProjectId}

import scala.concurrent.ExecutionContext
import scala.util.Success

trait Routes extends JsonSupport {
  this: ServerSetup =>

  def projectsManager: ActorRef

  private val withId = parameters("project-id".as[Int])

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
            projectsManager ! Disconnect(id)
            complete(StatusCodes.NoContent)
        }
      } ~
      path("stop-server") {
        projectsManager ! PoisonPill
        actorSystem.terminate()
        complete(StatusCodes.Accepted)
      } ~
      path("compile") {
        withId { id =>
          val fut = for {
              projectManagerOpt <- (projectsManager ? ProjectId(id)).mapTo[Option[ActorRef]]
              if projectManagerOpt.isDefined
              errors <- (projectManagerOpt.get ? CompileProject).mapTo[Seq[CompilerError]]
            } yield errors.toList
          onComplete(fut) {
          case Success(lst) => complete(lst)
          case _ => complete(StatusCodes.NotFound)
          }
        }
      }
    }
}
