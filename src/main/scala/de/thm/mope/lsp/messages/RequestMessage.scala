package de.thm.mope.lsp.messages

import spray.json.JsValue

case class RequestMessage(id:Int, method:String, params:JsValue, jsonrpc:String="2.0")
