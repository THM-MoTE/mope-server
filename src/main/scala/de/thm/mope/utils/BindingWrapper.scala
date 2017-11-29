package de.thm.mope.utils

import scala.concurrent.Future
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Tcp

sealed trait BindingWrapper {
  def unbind():Future[Unit]
}

case class HttpBinding(bnd:Http.ServerBinding)
    extends BindingWrapper {
  def unbind():Future[Unit] = bnd.unbind()
}

case class TcpBinding(bnd:Tcp.ServerBinding)
    extends BindingWrapper {
  def unbind():Future[Unit] = bnd.unbind()
}
