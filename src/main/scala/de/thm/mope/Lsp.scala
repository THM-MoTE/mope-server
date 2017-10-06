package de.thm.mope

import java.util.Objects

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream._
import akka.util.ByteString
import akka.stream.scaladsl._
import spray.json._
import com.typesafe.config.ConfigFactory
import de.thm.mope.lsp.LspServer
import de.thm.mope.lsp.messages.RequestMessage
import de.thm.mope.server.{ProjectsManagerActor, ServerSetup}

import scala.concurrent.{Future, Promise}
import scala.io.StdIn

object Lsp
    extends App
    with lsp.Routes
    with ServerSetup {

  override val interface = "127.0.0.1"
  override val port = 9010

  Objects.requireNonNull(getClass().getResource("/mope.conf"))
  Objects.requireNonNull(getClass().getResource("/logback.xml"))
  override implicit lazy val actorSystem = ActorSystem("lsp-test", ConfigFactory.load("mope.conf"))
  override val projectsManager: ActorRef = actorSystem.actorOf(Props[ProjectsManagerActor], name = "Root-ProjectsManager")
  override implicit lazy val materializer = ActorMaterializer()

  val server = new LspServer()

  val notifyActorPromise = Promise[ActorRef]()
  override def notificationActor = notifyActorPromise.future

  val pipeline = server.connectTo(routes).mapMaterializedValue { ref =>
    //one's the actor is materialized; resolve the notification promise
    notifyActorPromise.success(ref)
    ref
  }

   val connection = Tcp().bindAndHandle(pipeline, interface, port)

   connection.onComplete {
     case scala.util.Success(_) =>
       serverlog.info("LSP-TCP-Server running at {}:{}", interface, port)
     case scala.util.Failure(ex) =>
       serverlog.error("Failed to start server at {}:{} - {}", interface, port, ex.getMessage)
       actorSystem.terminate()
   }

   Future {
     serverlog.info("Press Ctrl+D to interrupt")
     while (System.in.read() != -1) {} //wait for Ctrl+D (end-of-transmission) ; EOT == -1 for JVM
     connection.
       flatMap(_.unbind()).
       onComplete(_ => actorSystem.terminate())
   }
}
