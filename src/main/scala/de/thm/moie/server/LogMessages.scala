package de.thm.moie.server

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.Logging

trait LogMessages extends Actor {
  val log = Logging(context.system, this)

  override abstract def receive: Receive = {
    case a:Any =>
      log.debug(s"Msg: $a")
      super.receive(a)
  }
}
