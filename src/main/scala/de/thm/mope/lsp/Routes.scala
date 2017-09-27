package de.thm.mope.lsp
import akka.stream.scaladsl._
import de.thm.mope.server.JsonSupport

trait Routes extends JsonSupport {
  def routes = (RpcHandler("compile")(
    Flow[Int].map(_*2)
  ) | RpcHandler("complete")(
    Flow[String].map(_.toUpperCase)
  )
  )
}
