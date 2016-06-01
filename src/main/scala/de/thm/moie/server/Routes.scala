package de.thm.moie.server

import akka.pattern.ask
import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import de.thm.moie.project.ProjectDescription
import de.thm.moie.project.ProjectDescription._
import de.thm.moie.server.ProjectsManagerActor.ProjectId

import scala.concurrent.ExecutionContext

trait Routes extends JsonSupport {
  this: ServerSetup =>

  def projectsManager: ActorRef

  val routes =
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
          complete(ProjectDescription("Dummy-URL", List()))
        }
      }
    }
}
