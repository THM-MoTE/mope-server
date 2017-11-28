package de.thm.mope.lsp

import java.net.URI

import de.thm.mope.lsp.messages._
import de.thm.mope.server.JsonSupport
import spray.json.{JsObject, JsString, JsValue, JsonWriter, RootJsonFormat}

import scala.util.Try

trait LspJsonSupport {
  this: JsonSupport =>

/*  implicit val unitReader = new JsonWriter[Unit] {
    override def write(obj: Unit) = JsObject()
  }*/
  implicit val rpcRequestFormat = jsonFormat4(RequestMessage)
  implicit val rpcNotificationFormat = jsonFormat3(NotificationMessage)
  implicit val rpcMessageFormat = new RootJsonFormat[RpcMessage] {
    override def read(json: JsValue):RpcMessage =
      Try[RpcMessage](rpcRequestFormat.read(json))
        .orElse(Try(rpcNotificationFormat.read(json)))
        .get

    override def write(obj: RpcMessage): JsValue = obj match {
      case x:RequestMessage => rpcRequestFormat.write(x)
      case x:NotificationMessage => rpcNotificationFormat.write(x)
    }
  }

  implicit val positionFormat = jsonFormat2(Position.apply)
  implicit val rangeFormat = jsonFormat2(Range)
  implicit val locationFormat = jsonFormat2(Location.apply)
  implicit val diagnosticFormat = jsonFormat4(Diagnostic.apply)
  implicit val completionFormat = jsonFormat4(CompletionItem.apply)
  implicit val respErrFormat = jsonFormat2(ResponseError.apply)
  implicit val respMsgFormat = jsonFormat4(ResponseMessage)
  implicit val initParamsFormat = jsonFormat5(InitializeParams)
  implicit val textDocIdentFormat = jsonFormat2(TextDocumentIdentifier)
  implicit val textDocumentPosFormat = jsonFormat2(TextDocumentPositionParams)
  implicit val didSaveNotifyFormat = jsonFormat1(DidSaveTextDocumentParams)
  implicit val documentChangeEvFormat = jsonFormat3(TextDocumentContentChangeEvent)
  implicit val documentChangeParFormat = jsonFormat2(DidChangeTextDocumentParams)
  implicit val hoverFormat = jsonFormat2(Hover)
}
