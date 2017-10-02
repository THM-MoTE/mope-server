package de.thm.mope.lsp
import akka.stream.scaladsl._
import de.thm.mope.server.JsonSupport
import spray.json._
import de.thm.mope.lsp.messages._

import scala.concurrent.Future

trait Routes extends JsonSupport {
  import RpcMethod._
  val initializeResponse =
    Map[String, JsValue](
      "textDocumentSync" -> 0.toJson,
      "completionProvider" -> JsObject("resolveProvider" -> false.toJson, "triggerCharacters" -> Seq(".").toJson),
      "definitionProvider" -> true.toJson,
    ).toJson

  def routes = request("compile"){ i:Int => Future.successful(i*2) } |
    request("complete"){ s:String => Future.successful(s.toUpperCase) } |
    (request("initialize") { params: InitializeParams =>
      Future.successful(JsObject("capabilities" -> initializeResponse))
    } | request("textDocument/completion") { params: TextDocumentPositionParams =>
      Future.successful(JsArray())
    } | notification("textDocument/didSave") { params: DidSaveTextDocumentParams =>
      Future.successful(())
    })
}
