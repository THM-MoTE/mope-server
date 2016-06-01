package de.thm.moie.server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps

trait ServerSetup {

  implicit val actorSystem = ActorSystem("moie-system")
  implicit val execContext = actorSystem.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val defaultTimeout = Timeout(3 seconds)
}
