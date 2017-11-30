package de.thm.mope.initializers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import de.thm.mope.config.ServerConfig
import de.thm.mope.server
import de.thm.mope.utils.{BindingWrapper, HttpBinding}

import scala.concurrent.Future

class HttpBindingProvider(router:server.Routes,
                          serverConfig:ServerConfig)
                         (implicit val mat:ActorMaterializer)
  extends BindingProvider {

  implicit val system = mat.system
  implicit val dispatcher = system.dispatcher

  override def provide():Future[BindingWrapper] =
    Http().bindAndHandle(router.routes, serverConfig.interface, serverConfig.port)
      .map(HttpBinding(_))
}
