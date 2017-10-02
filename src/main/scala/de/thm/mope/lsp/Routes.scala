package de.thm.mope.lsp
import akka.stream.scaladsl._
import de.thm.mope.server.JsonSupport
import spray.json._
import de.thm.mope.lsp.messages._

import scala.concurrent.Future

trait Routes extends JsonSupport {
  val initializeResponse =
    Map[String, JsValue](
      "textDocumentSync" -> 0.toJson,
      "completionProvider" -> JsObject("resolveProvider" -> false.toJson, "triggerCharacters" -> Seq(".").toJson),
      "definitionProvider" -> true.toJson,
    ).toJson

  def routes = RpcMethod("compile"){ i:Int => i*2 } |
    RpcMethod("complete"){ s:String => s.toUpperCase } |
    (RpcMethod("initialize") { params: InitializeParams =>
      JsObject("capabilities" -> initializeResponse)
    } | RpcMethod("textDocument/completion") { params: TextDocumentPositionParams =>
      JsArray()
    } | RpcMethod("textDocument/didSave") { params: DidSaveTextDocumentParams =>
      JsArray()
    })
}
