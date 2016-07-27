/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.util.NoSuchElementException

import akka.actor.{ActorRef, PoisonPill}
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{server, unmarshalling}
import akka.pattern.ask
import de.thm.moie.Global
import de.thm.moie.compiler.CompilerError
import de.thm.moie.declaration.DeclarationRequest
import de.thm.moie.doc.DocInfo._
import de.thm.moie.doc.DocumentationProvider.GetDocumentation
import de.thm.moie.doc.{DocInfo, DocumentationProvider}
import de.thm.moie.position.FilePath
import de.thm.moie.project._
import de.thm.moie.server.ProjectManagerActor.{CheckModel, CompileDefaultScript, CompileProject, CompileScript}
import de.thm.moie.server.ProjectsManagerActor.{Disconnect, ProjectId, RemainingClients}
import de.thm.moie.suggestion.{CompletionRequest, CompletionResponse}
import de.thm.moie.templates.TemplateEngine
import de.thm.moie.templates.TemplateEngine._
import de.thm.moie.utils.IOUtils

import scala.concurrent.Future

trait Routes extends JsonSupport with ErrorHandling {
  this: ServerSetup =>

  def projectsManager: ActorRef

  private val exitOnLastDisconnect =
    Global.config.getBoolean("exitOnLastDisconnect").getOrElse(false)

  private val cssStream = getClass.getResourceAsStream("/templates/style.css")
  private val docStream = getClass.getResourceAsStream("/templates/documentation.html")
  private val missingDocStream = getClass.getResourceAsStream("/templates/missing-doc.html")
  private val styleEngine = new TemplateEngine(IOUtils.toString(cssStream))
  private val docEngine = new TemplateEngine(IOUtils.toString(docStream)).merge(styleEngine, "styles")
  private val missingDocEngine = new TemplateEngine(IOUtils.toString(missingDocStream)).merge(styleEngine, "styles")

  private def createSubcomponentLink(comp: DocInfo.Subcomponent, link:String): String =
    s"""<li>
    |  <a href="$link">${comp.className}</a> ${comp.classComment.map("- "+_).getOrElse("")}
    |</li>""".stripMargin

  private def shutdown(cause:String): Unit = {
    actorSystem.terminate()
    serverlog.info("Shutdown because {}", cause)
  }

  private def disconnectWithExit(id:Int):Unit =
    (projectsManager ? Disconnect(id)).
      mapTo[Option[RemainingClients]].
      collect {
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

  private def postEntity[T](tpe: unmarshalling.FromRequestUnmarshaller[T]):server.Directive1[T] =
    post & entity(tpe)

  private def postEntityWithId[T,Z : ToEntityMarshaller](tpe: unmarshalling.FromRequestUnmarshaller[T], id:Int)(fn: (T, ActorRef) => Future[Z]) =
    postEntity(tpe) { t =>
      withIdExists(id) { ref =>
        fn(t,ref)
      }
    }

  def routes =
    handleExceptions(exceptionHandler) {
      pathPrefix("moie") {
        path("connect") {
          postEntity(as[ProjectDescription]) { description =>
            onSuccess(for {
              eitherId <- (projectsManager ? description).mapTo[Either[List[String], ProjectId]]
            } yield eitherId) {
              case Left(errors) => complete(StatusCodes.BadRequest, errors)
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
        postEntityWithId(as[FilePath], id) { (filepath, projectManager) =>
          (projectManager ? CompileProject(filepath)).mapTo[Seq[CompilerError]]
        }
      } ~
      path("compileScript") {
        postEntityWithId(as[FilePath], id) { (filepath, projectManager) =>
          (projectManager ? CompileScript(filepath)).mapTo[Seq[CompilerError]]

        } ~
        get {
          withIdExists(id) { projectManager =>
            (projectManager ? CompileDefaultScript).mapTo[Seq[CompilerError]]
          }
        }
      } ~
      path("checkModel") {
        postEntityWithId(as[FilePath], id) { (filepath, projectManager) =>
          (projectManager ? CheckModel(filepath)).mapTo[String]
        }
      } ~
      path("completion") {
        postEntityWithId(as[CompletionRequest], id) { (completion, projectManager) =>
          (projectManager ? completion).mapTo[Set[CompletionResponse]]
        }
      } ~
      (path("declaration")  & get & parameters("class")) { clazz =>
        withIdExists(id) { projectManager =>
          (projectManager ? DeclarationRequest(clazz)).
            mapTo[Option[FilePath]].
            flatMap {
              case Some(path) => Future.successful(path)
              case None => Future.failed(new NotFoundException(s"class ${clazz} not found"))
            }
        }
      } ~
      (path("doc") & get & parameters("class") & extractUri) { (clazz, uri) =>
        withIdExists(id) { projectManager =>
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
