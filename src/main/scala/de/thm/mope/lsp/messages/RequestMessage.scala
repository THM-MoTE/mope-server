package de.thm.mope.lsp.messages

import spray.json.JsValue

sealed trait RpcMessage {
  def method:String
  def params:JsValue
  def jsonrpc:String="2.0"
}
case class RequestMessage(id:Int, override val method:String, override val params:JsValue, override val jsonrpc:String="2.0")
  extends RpcMessage
case class NotificationMessage(override val method:String, override val params:JsValue, override val jsonrpc:String="2.0")
  extends RpcMessage
