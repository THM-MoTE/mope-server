package de.thm.mope.lsp
import java.nio.file.Paths

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.scaladsl._
import de.thm.mope.compiler.CompilerError
import de.thm.mope.server.{JsonSupport, ServerSetup}
import spray.json._
import de.thm.mope.lsp.messages._
import de.thm.mope.project.ProjectDescription
import de.thm.mope.server.ProjectManagerActor.CompileProject
import de.thm.mope.server.ProjectsManagerActor.ProjectId

import scala.concurrent.{Future, Promise}

trait Routes extends JsonSupport {
  this: ServerSetup =>
  import RpcMethod._

  def notificationActor:Future[ActorRef]
    //manager of initialized project
  val projectManagerPromise = Promise[ActorRef]
  def askProjectManager(x:Any):Future[Any] =
    projectManagerPromise.future
    .flatMap(_ ? x)

  val initializeResponse =
    Map[String, JsValue](
      "textDocumentSync" -> 0.toJson,
      "completionProvider" -> JsObject("resolveProvider" -> false.toJson, "triggerCharacters" -> Seq(".").toJson),
      "definitionProvider" -> true.toJson,
    ).toJson

  def routes = request("compile"){ i:Int => Future.successful(i*2) } |
    request("complete"){ s:String => Future.successful(s.toUpperCase) } |
    (request("initialize") { params: InitializeParams =>
      (projectsManager ? ProjectDescription(params.projectFolder, "target", None)).mapTo[Either[List[String], ProjectId]]
        .flatMap {
          case Left(lst) => Future.failed(InitializeException(lst.mkString("\n")))
          case Right(id) => (projectsManager ? id).mapTo[Option[ActorRef]].map(_.get) //must be defined
        }
        .map { ref =>
          //save initiailzed manager
          projectManagerPromise.success(ref)
          JsObject("capabilities" -> initializeResponse)
        }
    } | request("textDocument/completion") { params: TextDocumentPositionParams =>
      Future.successful(JsArray())
    } | notification("textDocument/didSave") { params: DidSaveTextDocumentParams =>
      Future.successful(())
    })
}
