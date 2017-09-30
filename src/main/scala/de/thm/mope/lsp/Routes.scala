package de.thm.mope.lsp
import akka.stream.scaladsl._
import de.thm.mope.server.JsonSupport
import spray.json._

trait Routes extends JsonSupport {

  val initializeResponse =
    Map[String, JsValue](
      "textDocumentSync" -> 0.toJson,
      "completionProvider" -> JsObject("resolveProvider" -> false.toJson, "triggerCharacters" -> Seq(".").toJson),
      "definitionProvider" -> true.toJson,
    ).toJson

  def routes = RpcMethod("compile")(
    Flow[Int].map(_*2)
  ) | RpcMethod("complete")(
    Flow[String].map(_.toUpperCase)
  ) | RpcMethod("initialize") (
    Flow[JsValue].map(_ => JsObject("capabilities" ->initializeResponse))
  )
}
