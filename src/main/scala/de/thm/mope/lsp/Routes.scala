package de.thm.mope.lsp
import java.net.URI
import java.nio.file.Paths

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.stream.scaladsl._
import de.thm.mope.compiler.CompilerError
import de.thm.mope.declaration.DeclarationRequest
import de.thm.mope.server.{JsonSupport, NotFoundException, ServerSetup}
import spray.json._
import de.thm.mope.lsp.messages._
import de.thm.mope.position.{CursorPosition, FileWithLine}
import de.thm.mope.project.ProjectDescription
import de.thm.mope.server.ProjectManagerActor.CompileProject
import de.thm.mope.server.ProjectsManagerActor.ProjectId
import de.thm.mope.suggestion.{CompletionRequest, Suggestion}
import de.thm.mope.utils._

import scala.concurrent.{Future, Promise}
import scala.reflect.ClassTag

trait Routes extends JsonSupport with LspJsonSupport {
  this: ServerSetup =>
  import RpcMethod._

  //TODO: split notificatinos & requests into 2 handlers

  def notificationActor:Future[ActorRef]
  lazy val bufferActor = actorSystem.actorOf(Props[BufferContentActor], "BCA")

    //manager of initialized project
  val projectManagerPromise = Promise[ActorRef]
  def askProjectManager[S:ClassTag](x:Any):Future[S] =
    projectManagerPromise.future
    .flatMap(_ ? x)
    .mapTo[S]

  val initializeResponse =
    Map[String, JsValue](
      "textDocumentSync" -> 1.toJson,
      "completionProvider" -> JsObject("resolveProvider" -> false.toJson, "triggerCharacters" -> Seq(".").toJson),
      "definitionProvider" -> true.toJson,
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
    } |: request[TextDocumentPositionParams, JsObject]("textDocument/completion") { case TextDocumentPositionParams(textDocument, position) =>
        (bufferActor ? BufferContentActor.GetWord(textDocument.path, position)).mapTo[Option[String]]
      .flatMap {
        case Some(word) =>
          askProjectManager[Set[Suggestion]](CompletionRequest(textDocument.uri.getRawPath, position.filePosition,word))
            .map { set =>
              val items = set.map(CompletionItem(word,_)).toList
              JsObject("isIncomplete" -> false.toJson, "items" -> items.toJson)
            }
        case None =>
          Future.successful(JsObject("isIncomplete" -> false.toJson, "items" -> Seq.empty[CompletionItem].toJson))
      }
    } |: request[TextDocumentPositionParams, Seq[Location]]("textDocument/definition") { case TextDocumentPositionParams(textDocument, position) =>
      (for {
          optWord <- (bufferActor ? BufferContentActor.GetWord(textDocument.path, position)).mapTo[Option[String]]
          word <- optionToNotFoundExc(optWord, s"Don't know the word below the cursor:(")
          optFile <- askProjectManager[Option[FileWithLine]](DeclarationRequest(CursorPosition(textDocument.uri.getRawPath, position.filePosition, word)))
          file <- optionToNotFoundExc(optFile, s"Can't find a definition :(")
        } yield Seq(Location(file)))
        .recover {
          case NotFoundException(_) => Seq.empty[Location]
        }
    } |: notification("textDocument/didSave") { params: DidSaveTextDocumentParams =>
        askProjectManager[Seq[CompilerError]](CompileProject(params.textDocument.path))
        .map { seq =>
          //TODO: send empty errors to client to clear the error list
           val errMap = seq.map { //to lsp Diagnostic
              case CompilerError("Error", file, start,end,msg) =>
                (Paths.get(file).toUri, Diagnostic(Range(Position(start),Position(end)),
                  Diagnostic.Error, file, file, msg))
              case CompilerError("Warning", file, start,end,msg) =>
                (Paths.get(file).toUri, Diagnostic(Range(Position(start),Position(end)),
                  Diagnostic.Warning, file, file, msg))
            }.groupBy(_._1)

           errMap.foreach { case (fileUri, errors) =>
             val diagnostics = errors.map(_._2)
             notificationActor.foreach(_ ! NotificationMessage("textDocument/publishDiagnostics",JsObject("uri" -> fileUri.toJson, "diagnostics" -> diagnostics.toJson)))
           }
        }
    } |: notification("textDocument/didChange") { change:DidChangeTextDocumentParams =>
      bufferActor ! BufferContentActor.BufferContent(change.textDocument.path, change.contentChanges.head.text)
      Future.successful(())
    }
}
