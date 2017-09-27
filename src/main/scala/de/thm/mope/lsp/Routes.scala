package de.thm.mope.lsp
import akka.stream.scaladsl._
import de.thm.mope.server.JsonSupport

trait Routes extends LspRoutes {
  def routes: RpcMethod[_,_] =
    ("compile" -> Flow[Int].map(_*2)) |
    ("complete" -> Flow[String].map(_.toUpperCase))
}
