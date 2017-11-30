package de.thm.mope.lsp

import java.net.URI
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.stream.scaladsl._
import akka.util.Timeout
import de.thm.mope.compiler.CompilerError
import de.thm.mope.declaration.DeclarationRequest
import de.thm.mope.server.{JsonSupport, NotFoundException}
import de.thm.mope.doc.{DocInfo, DocumentationProvider}
import spray.json._
import de.thm.mope.lsp.messages._
import de.thm.mope.position.{CursorPosition, FileWithLine}
import de.thm.mope.project.ProjectDescription
import de.thm.mope.server.ProjectManagerActor.CompileProject
import de.thm.mope.server.ProjectsManagerActor.ProjectId
import de.thm.mope.suggestion.{CompletionRequest, Suggestion}
import de.thm.mope.utils._
import de.thm.mope._
import de.thm.mope.config.ServerConfig

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.reflect.ClassTag
import cats.data.OptionT
import cats.implicits._

class Routes(
  projectsManager: ProjectsManagerRef,
  notificationActor:Future[NotifyActorRef],
  bufferActor:BufferActorRef)
  (implicit context:ExecutionContext,
            timeout:Timeout)
    extends JsonSupport
    with LspJsonSupport {
  import RpcMethod._

    //manager of initialized project
  val projectManagerPromise = Promise[ActorRef]
  def askProjectManager[S:ClassTag](x:Any):Future[S] =
    projectManagerPromise.future
    .flatMap(_ ? x)
    .mapTo[S]

  val initializeResponse =
    Map[String, JsValue](
      "textDocumentSync" -> 1.toJson,
      "hoverProvider" -> true.toJson,
      "completionProvider" -> JsObject("resolveProvider" -> false.toJson, "triggerCharacters" -> Seq(".").toJson),
      "definitionProvider" -> true.toJson
    ).toJson

  def routes =
    request("initialize") { params: InitializeParams =>
      (projectsManager ? ProjectDescription(params.projectFolder.getRawPath, "target", None)).mapTo[Either[List[String], ProjectId]]
        .flatMap {
          case Left(lst) => Future.failed(InitializeException(lst.mkString("\n")))
          case Right(id) => (projectsManager ? id).mapTo[Option[ActorRef]].map(_.get) //must be defined
        }
        .map { ref =>
          //save initiailzed manager
          projectManagerPromise.success(ref)
          JsObject("capabilities" -> initializeResponse)
        }
    } || features || notifications



  def features = {
     request[TextDocumentPositionParams, JsObject]("textDocument/completion") { case TextDocumentPositionParams(textDocument, position) =>
        (bufferActor ? BufferContentActor.GetWord(textDocument.path, position)).mapTo[Option[String]]
      .flatMap {
        case Some(word) =>
          askProjectManager[Seq[Suggestion]](CompletionRequest(textDocument.uri.getRawPath, position.filePosition,word))
            .map { set =>
              val items = set.map(CompletionItem(word,_)).toList
              JsObject("isIncomplete" -> false.toJson, "items" -> items.toJson)
            }
        case None =>
          Future.successful(JsObject("isIncomplete" -> false.toJson, "items" -> Seq.empty[CompletionItem].toJson))
      }
     } ||
    request[TextDocumentPositionParams, Seq[Location]]("textDocument/definition") { case TextDocumentPositionParams(textDocument, position) =>
      (for {
        word <- OptionT((bufferActor ? BufferContentActor.GetWord(textDocument.path, position)).mapTo[Option[String]])
        file <- OptionT(askProjectManager[Option[FileWithLine]](DeclarationRequest(CursorPosition(textDocument.uri.getRawPath, position.filePosition, word))))
      } yield Seq(Location(file)))
        .value
        .map(_.getOrElse(Seq.empty[Location]))
    } ||
    request[TextDocumentPositionParams, Hover]("textDocument/hover") { case TextDocumentPositionParams(textDocument, position) =>
      (for {
        word <- OptionT((bufferActor ? BufferContentActor.GetWord(textDocument.path, position)).mapTo[Option[String]])
        doc <- OptionT(askProjectManager[Option[DocInfo]](DocumentationProvider.GetDocumentation(word)))
      } yield Hover(Seq(doc.info)))
        .value
        .map(_.getOrElse(Hover(Seq.empty[String])))
    }
  }

  def notifications =
     notification("textDocument/didSave") { params: DidSaveTextDocumentParams =>
        askProjectManager[Seq[CompilerError]](CompileProject(params.textDocument.path))
          .map { seq =>
          val errMap = if(seq.nonEmpty) {
            seq.map { //to lsp Diagnostic
              case CompilerError("Error", file, start,end,msg) =>
                (Paths.get(file).toUri, Diagnostic(Range(Position(start),Position(end)),
                  Diagnostic.Error, "modelica", msg))
              case CompilerError("Warning", file, start,end,msg) =>
                (Paths.get(file).toUri, Diagnostic(Range(Position(start),Position(end)),
                  Diagnostic.Warning, "modelica", msg))
            }.groupBy(_._1) //group by file uri
              .mapValues{ seq => seq.map(_._2) } //remove uri from values
          } else {
            //if there are no errors: at least answer with no errors
            Map(params.textDocument.uri -> List.empty[Diagnostic])
          }

          errMap.foreach { case (fileUri, diagnostics) =>
             notificationActor.foreach(_ ! NotificationMessage("textDocument/publishDiagnostics",JsObject("uri" -> fileUri.toJson, "diagnostics" -> diagnostics.toJson)))
           }
        }
    } || notification("textDocument/didChange") { change:DidChangeTextDocumentParams =>
      bufferActor ! BufferContentActor.BufferContent(change.textDocument.path, change.contentChanges.head.text)
      Future.successful(())
    }

}
