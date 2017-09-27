package de.thm.mope.lsp
import akka.stream.scaladsl._
import de.thm.mope.server.JsonSupport

trait Routes extends JsonSupport {
  def routes = (RpcMethod("compile")(
    Flow[Int].map(_*2)
  ) | RpcMethod("complete")(
    Flow[String].map(_.toUpperCase)
  )
  )
}
