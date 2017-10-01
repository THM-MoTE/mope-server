package de.thm.mope.lsp.messages

import spray.json.JsValue

case class ResponseMessage(id:Int, result:Option[JsValue], error:Option[ResponseError], jsonrpc:String="2.0")
case class ResponseError(code:Int,message:String)
