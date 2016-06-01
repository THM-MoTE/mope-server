package de.thm.moie.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.sys.process._
import scala.concurrent.Future
import java.io._
import akka.actor.Props

class Server(port:Int) extends Routes {

  private val escCode = 0x1b

  implicit val actorSystem = ActorSystem("moie-system")
  implicit val materializer = ActorMaterializer()
  implicit val context = actorSystem.dispatcher

  val bindingFuture = Http().bindAndHandle(routes, "localhost", port)
  println(s"Server running at localhost:$port")
  println("Press Enter to interrupt")
  var char = StdIn.readLine()

  bindingFuture.
    flatMap(_.unbind()).
    onComplete(_ => actorSystem.terminate())
}
