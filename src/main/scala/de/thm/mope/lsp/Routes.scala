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

trait Routes extends JsonSupport {
  this: ServerSetup =>
  import RpcMethod._

  def notificationActor:Future[ActorRef]
  lazy val bufferActor = actorSystem.actorOf(Props[BufferContentActor], "BCA")

    //manager of initialized project
  val projectManagerPromise = Promise[ActorRef]
  def askProjectManager(x:Any):Future[Any] =
    projectManagerPromise.future
    .flatMap(_ ? x)

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
    } |: request("textDocument/completion") { params: TextDocumentPositionParams =>
      askProjectManager(params.toCompletionRequest).mapTo[Set[Suggestion]]
      .map { set =>
        val items = set.map { sug =>
          CompletionItem(sug)
        }.toList
        JsObject("isIncomplete" -> false.toJson, "items" -> items.toJson)
      }
    } |: notification("textDocument/didSave") { params: DidSaveTextDocumentParams =>
        askProjectManager(CompileProject(Paths.get(params.textDocument.uri))).mapTo[Seq[CompilerError]]
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
    }
}
