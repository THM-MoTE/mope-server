package de.thm.mope.lsp.messages

import spray.json.JsValue

case class ResponseMessage(id:Int, result:JsValue, jsonrpc:String="2.0")
