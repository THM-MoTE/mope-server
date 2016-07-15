/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file.Paths
import java.util.NoSuchElementException

import akka.actor.{ActorRef, PoisonPill}
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.pattern.ask
import de.thm.moie.Global
import de.thm.moie.compiler.CompilerError
import de.thm.moie.project._
import de.thm.moie.server.ProjectManagerActor.{CheckModel, CompileDefaultScript, CompileProject, CompileScript}
import de.thm.moie.server.ProjectsManagerActor.{Disconnect, ProjectId, RemainingClients}

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait Routes extends JsonSupport with ErrorHandling {
  this: ServerSetup =>

  def projectsManager: ActorRef

  private val exitOnLastDisconnect =
    Global.config.getBoolean("exitOnLastDisconnect").getOrElse(false)

  private def shutdown(cause:String="unkown cause"): Unit = {
    actorSystem.terminate()
    serverlog.info("Shutdown because {}", cause)
  }

  private def disconnectWithExit(id:Int):Unit =
    (projectsManager ? Disconnect(id)).
      mapTo[Option[RemainingClients]].collect {
        case Some(RemainingClients(0)) if exitOnLastDisconnect => ()
      }.foreach { _ =>
        shutdown("no active clients left")
      }

  private def withIdExists[T : ToEntityMarshaller](id:Int)(fn: ActorRef => Future[T]) = {
      val projectsManagerOpt =  (projectsManager ? ProjectId(id)).mapTo[Option[ActorRef]]
      val future = projectsManagerOpt.collect {
        case Some(ref) => ref
        case None => throw new NoSuchElementException(s"unknown project-id $id")
      }.flatMap(fn)
      complete(future)
    }

  def routes =
    handleExceptions(exceptionHandler) {
      pathPrefix("moie") {
        path("connect") {
          post {
            entity(as[ProjectDescription]) { description =>
              onSuccess(for {
                eitherId <- (projectsManager ? description).mapTo[Either[List[String], ProjectId]]
              } yield eitherId) {
                case Left(errors) => complete(StatusCodes.BadRequest, errors)
                case Right(projId) => complete(IntJsonFormat.write(projId.id))
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
        projectRoutes
      }
    }

  def projectRoutes =
    pathPrefix("project" / IntNumber) { id =>
      path("disconnect") {
        post {
          disconnectWithExit(id)
          complete(StatusCodes.NoContent)
        }
      } ~
        path("compile") {
          post {
            entity(as[FilePath]) { filepath =>
              withIdExists(id) { projectManager =>
                (projectManager ? CompileProject(filepath)).mapTo[Seq[CompilerError]]
              }
            }
          }
        } ~
        path("compileScript") {
          post {
            entity(as[FilePath]) { filepath =>
              withIdExists(id) { projectManager =>
                (projectManager ? CompileScript(filepath)).mapTo[Seq[CompilerError]]
              }
            }
          } ~ get {
            withIdExists(id) { projectManager =>
              (projectManager ? CompileDefaultScript).mapTo[Seq[CompilerError]]
            }
          }
        } ~
        path("checkModel") {
          post {
            entity(as[FilePath]) { filepath =>
              withIdExists(id) { projectManager =>
                (projectManager ? CheckModel(filepath)).mapTo[String]
              }
            }
          }
        } ~
        path("completion") {
          post {
            entity(as[CompletionRequest]) { completion =>
              withIdExists(id) { projectManager =>
                (projectManager ? completion).mapTo[Set[CompletionResponse]]
            }
          }
        }
      } ~
      path("declaration") {
        post {
          entity(as[DeclarationRequest]) { declReq =>
            withIdExists(id) { projectManager =>
              (projectManager ? declReq).mapTo[Option[FilePath]].
                flatMap {
                  case Some(path) => Future.successful(path)
                  case None => Future.failed(new NotFoundException(s"class ${declReq.className} not found"))
                }
            }
          }
        }
      }
    }
}
