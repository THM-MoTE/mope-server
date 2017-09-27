package de.thm.mope.lsp

import akka.stream.scaladsl.Flow
import de.thm.mope.server.JsonSupport

trait LspRoutes
  extends JsonSupport {

  def routes[I,O]: RpcMethod[I,O]

  implicit def fromTuple[I,O, Mat](tuple:(String, Flow[I,O, Mat])): RpcMethod[I,O] = {
    val (methodName, flow) = tuple
    RpcMethod(methodName, flow)
  }
}
