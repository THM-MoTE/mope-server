package de.thm.mope.initializers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp
import com.softwaremill.macwire._
import com.softwaremill.tagging._
import de.thm.mope.config.ServerConfig
import de.thm.mope.lsp.LspJsonSupport
import de.thm.mope.server.JsonSupport
import de.thm.mope.tags.NotifyActorMarker
import de.thm.mope.utils.{BindingWrapper, TcpBinding}
import de.thm.mope.{BufferActorRef, NotifyActorRef, ProjectsManagerRef, lsp}

import scala.concurrent.{Future, Promise}

class LspBindingProvider(
                          projectsManager: ProjectsManagerRef,
                          bufferActor:BufferActorRef,
                          serverConfig:ServerConfig)
                        (implicit val mat:ActorMaterializer)
extends BindingProvider
with JsonSupport
with LspJsonSupport {

  implicit val system = mat.system
  implicit val dispatcher = system.dispatcher
  import serverConfig.timeout

  override def provide(): Future[BindingWrapper] = {
    lazy val lspServer:lsp.LspServer = wire[lsp.LspServer]
    val notifyActorPromise = Promise[NotifyActorRef]()
    val actorFuture:Future[NotifyActorRef] = notifyActorPromise.future
    lazy val router:lsp.Routes = wire[lsp.Routes]
    val pipeline = lspServer.connectTo(router.routes).mapMaterializedValue { ref =>
      //one's the actor is materialized; resolve the notification promise
      notifyActorPromise.success(ref.taggedWith[NotifyActorMarker])
      ref
    }

    Tcp().bindAndHandle(pipeline, serverConfig.interface, serverConfig.port)
      .map(TcpBinding(_))
  }
}
