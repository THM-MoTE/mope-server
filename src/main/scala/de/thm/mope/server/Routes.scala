/**
  * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */

package de.thm.mope.server

import java.nio.file.Path
import java.util.NoSuchElementException

import akka.actor.Status.Success
import akka.actor.{ActorRef, PoisonPill}
import akka.event.Logging
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{server, unmarshalling}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import de.thm.mope.ProjectsManagerRef
import de.thm.mope.compiler.CompilerError
import de.thm.mope.config.ServerConfig
import de.thm.mope.declaration.DeclarationRequest
import de.thm.mope.doc.DocInfo
import de.thm.mope.doc.DocInfo._
import de.thm.mope.doc.DocumentationProvider.GetDocumentation
import de.thm.mope.models.{SimulateRequest, SimulationResult}
import de.thm.mope.position._
import de.thm.mope.project._
import de.thm.mope.server.ProjectManagerActor._
import de.thm.mope.server.ProjectsManagerActor.{Disconnect, ProjectId, RemainingClients}
import de.thm.mope.server.RecentFilesActor._
import de.thm.mope.suggestion.{CompletionRequest, Suggestion, TypeOf, TypeRequest}
import de.thm.mope.templates.TemplateModule._
import de.thm.recent.JsProtocol._

import scala.concurrent.Future

class Routes(
              projectsManager: ProjectsManagerRef,
              servConf: ServerConfig,
              docEngine: DocTemplate,
              missingDocEngine: MissingDocTemplate,
              override val ensembleHandler: EnsembleHandler)(
              implicit
              mat: ActorMaterializer)
  extends JsonSupport
    with ErrorHandling
    with EnsembleRoutes {

  import servConf.timeout

  override val serverlog = Logging(mat.system, classOf[Routes])
  implicit val dispatcher = mat.system.dispatcher

  private val exitOnLastDisconnect =
    servConf.config.getBoolean("exitOnLastDisconnect")

  private def createSubcomponentLink(comp: DocInfo.Subcomponent, link: String): String =
    s"""<li>
       |  <a href="$link">${comp.className}</a> ${comp.classComment.map("- " + _).getOrElse("")}
       |</li>""".stripMargin

  private def shutdown(cause: String): Unit = {
    mat.system.terminate()
    serverlog.info("Shutdown because {}", cause)
  }

  private def disconnectWithExit(id: Int): Unit =
    (projectsManager ? Disconnect(id)).
      mapTo[Option[RemainingClients]].
      collect {
        case Some(RemainingClients(0)) if exitOnLastDisconnect => ()
      }.foreach { _ =>
      shutdown("no active clients left")
    }

  private def projectManager:Directive1[ActorRef] = {
    pathPrefix("project" / IntNumber).flatMap { id =>
      val projectsManagerOpt = (projectsManager ? ProjectId(id)).mapTo[Option[ActorRef]]
      val future = projectsManagerOpt.collect {
        case Some(ref) => ref
        case None => throw new NoSuchElementException(s"unknown project-id $id")
      }
      onSuccess(future)
    }
  }

  private def postEntity[T](tpe: unmarshalling.FromRequestUnmarshaller[T]): server.Directive1[T] =
    post & entity(tpe)

  def routes =
    handleExceptions(exceptionHandler) {
      pathPrefix("mope") {
        path("connect") {
          postEntity(as[ProjectDescription]) { description =>
            onSuccess(for {
              eitherId <- (projectsManager ? description).mapTo[Either[List[String], ProjectId]]
            } yield eitherId) {
              case Left(errors) => complete(HttpResponse(StatusCodes.BadRequest, entity = errors.mkString("\n")))
              case Right(projId) => complete(IntJsonFormat.write(projId.id))
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
          (path("recent-files") & get) {
            val lstFuture = (projectsManager ? GetRecentFiles).mapTo[Seq[Path]]
            onSuccess(lstFuture) { lst => complete(lst) }
          } ~
          projectRoutes ~
          ensembleRoutes
      }
    }

  def projectRoutes =
    pathPrefix("project" / IntNumber) { id =>
      (path("disconnect") & post) {
        disconnectWithExit(id)
        complete(StatusCodes.NoContent)
      }
    } ~
    projectManager { projectManager =>
        path("compile") {
          postEntity(as[FilePath]) { filepath =>
            complete {
              (projectManager ? CompileProject(filepath)).mapTo[Seq[CompilerError]]
            }
          }
        } ~
        path("compileScript") {
          postEntity(as[FilePath]) { filepath =>
            complete {
              (projectManager ? CompileScript(filepath)).mapTo[Seq[CompilerError]]
            }
          } ~
            get {
              complete {
                (projectManager ? CompileDefaultScript).mapTo[Seq[CompilerError]]
              }
            }
        } ~
        path("checkModel") {
          postEntity(as[FilePath]) { filepath =>
            complete {
              (projectManager ? CheckModel(filepath)).mapTo[String]
            }
          }
        } ~
      pathPrefix("simulate") {
        (get & pathPrefix(Remaining) & extractUri) { (id, uri) =>
          val eitherF = (projectManager ? SimulateActor.SimulationId(id)).mapTo[Either[SimulateActor.NotFinished,SimulationResult]]
          onSuccess(eitherF) {
            case Left(nf) => complete(HttpResponse(StatusCodes.Conflict, entity = nf.message).withHeaders(headers.Location(uri)))
            case Right(result) => complete(result)
          }
          } ~
          (postEntity(as[SimulateRequest]) & extractUri) { (req, uri) =>
            complete(for {
              opts <- req.convertOptions
              id <- (projectManager ? SimulateActor.SimulateModel(req.modelName, opts)).mapTo[SimulateActor.SimulationId]
              idUri = Uri(id.id).resolvedAgainst(uri+"/")
            } yield HttpResponse(StatusCodes.Accepted, entity = idUri.toString).withHeaders(headers.Location(idUri)))
          }
        } ~
        path("completion") {
          postEntity(as[CompletionRequest]) { completion =>
            complete {
              (projectManager ? completion).mapTo[Seq[Suggestion]]
            }
          }
        } ~
        path("typeOf") {
          postEntity(as[TypeRequest]) { typeOf =>
            complete {
              (projectManager ? typeOf).mapTo[Option[TypeOf]].
                flatMap(optionToNotFoundExc(_, s"type of ${typeOf.word} is unknown"))
            }
          }
        } ~
        path("declaration") {
          postEntity(as[CursorPosition]) { cursor =>
            complete {
              (projectManager ? DeclarationRequest(cursor)).
                mapTo[Option[FileWithLine]].
                flatMap(optionToNotFoundExc(_, s"declaration of ${cursor.word} not found"))
            }
          }
        } ~
        (get & path("doc") & parameters("class") & extractUri) { (clazz, uri) =>
          complete {
            (projectManager ? GetDocumentation(clazz)).
              mapTo[Option[DocInfo]].
              map {
                case Some(DocInfo(info, rev, header, subcomponents)) =>
                  val subcomponentEntries = subcomponents.toList.sorted.map { component =>
                    val link = uri.withQuery(Uri.Query("class" -> component.className)).toString
                    createSubcomponentLink(component, link)
                  }
                  val content = docEngine.insert(Map(
                    "className" -> clazz,
                    "subcomponents" -> "",
                    "info-header" -> header,
                    "info-string" -> info,
                    "revisions" -> rev,
                    "subcomponents" -> subcomponentEntries.mkString("\n")
                  )).getContent
                  HttpEntity(ContentTypes.`text/html(UTF-8)`, content)
                case None =>
                  val docMissing = missingDocEngine.insert(Map(
                    "className" -> clazz
                  )).getContent
                  HttpEntity(ContentTypes.`text/html(UTF-8)`, docMissing)
              }
          }
        }
    }
}
