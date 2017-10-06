package de.thm.mope.lsp
import java.net.URI
import java.nio.file.Paths

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.stream.scaladsl._
import de.thm.mope.compiler.CompilerError
import de.thm.mope.server.{JsonSupport, ServerSetup}
import spray.json._
import de.thm.mope.lsp.messages._
import de.thm.mope.project.ProjectDescription
import de.thm.mope.server.ProjectManagerActor.CompileProject
import de.thm.mope.server.ProjectsManagerActor.ProjectId
import de.thm.mope.suggestion.{CompletionRequest, Suggestion}

import scala.concurrent.{Future, Promise}
import scala.reflect.ClassTag

trait Routes extends JsonSupport {
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

  def routes = request("compile"){ i:Int => Future.successful(i*2) } |:
    request("complete"){ s:String => Future.successful(s.toUpperCase) } |:
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
      (bufferActor ? BufferContentActor.GetWord(position)).mapTo[Option[String]]
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
    } |: notification("textDocument/didSave") { params: DidSaveTextDocumentParams =>
        askProjectManager[Seq[CompilerError]](CompileProject(params.textDocument.path))
        .map { seq =>
          seq.map { //to lsp Diagnostic
            case CompilerError("Error", file, start,end,msg) =>
              (Paths.get(file).toUri, Diagnostic(Range(Position(start),Position(end)),
                Diagnostic.Error, file, file, msg))
          }
          .foreach { //publish each error
              case (fileName, diagnostic) =>
                notificationActor.foreach(_ ! NotificationMessage("textDocument/publishDiagnostics",JsObject("uri" -> fileName.toJson, "diagnostics" -> Seq(diagnostic).toJson)))
          }
        }
    } |: notification("textDocument/didChange") { change:DidChangeTextDocumentParams =>
      bufferActor ! BufferContentActor.BufferContent(change.textDocument.path, change.contentChanges.head.text)
      Future.successful(())
    }
}
