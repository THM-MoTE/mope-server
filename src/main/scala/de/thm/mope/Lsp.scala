package de.thm.mope

import java.util.Objects

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream._
import akka.util.ByteString
import akka.stream.scaladsl._
import spray.json._
import com.typesafe.config.ConfigFactory
import de.thm.mope.lsp.{LspServer, RpcMsg}

object Lsp
    extends App
    with server.JsonSupport
    with lsp.Routes {

  val interface = "127.0.0.1"
  val port = 9010

  Objects.requireNonNull(getClass().getResource("/mope.conf"))
  Objects.requireNonNull(getClass().getResource("/logback.xml"))
  implicit val system = ActorSystem("lsp-test", ConfigFactory.load("mope.conf"))
  implicit val context =  system.dispatcher
  implicit val mat = ActorMaterializer()
  val log = Logging(system, getClass)

  val server = new LspServer()

  val pipeline = server.connectTo(routes)

//  val connection = Tcp().bindAndHandle(pipeline, interface, port)
//
//  connection.onComplete {
//    case scala.util.Success(_) =>
//      log.info("LSP-TCP-Server running at {}:{}", interface, port)
//    case scala.util.Failure(ex) =>
//      log.error("Failed to start server at {}:{} - {}", interface, port, ex.getMessage)
//      system.terminate()
//  }
//
//  Future {
//    log.info("Press Enter to interrupt")
//    StdIn.readLine()
//    connection.
//      flatMap(_.unbind()).
//      onComplete(_ => system.terminate())
//  }

  log.debug("running the stream")

  Source(List(RpcMsg(1, "compile", 50.toJson)))//, RpcMsg(2, "complete", "nico".toJson)))
    .map { msg =>
      ByteString(s"""
         |Content-Type: text/utf-8
         |Content-Length: 58
         |
         |${msg.toJson}""".stripMargin)
    }
    .via(pipeline)
    .map(_.utf8String)
    .runForeach(println)
    .onComplete(_ => system.terminate())
 }
