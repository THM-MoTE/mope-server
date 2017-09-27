package de.thm.mope.lsp

import spray.json.JsValue

case class RpcMsg(id:Int, method:String, params:JsValue, jsonrpc:String="2.0")
