package de.thm.mope.utils.actors

import akka.actor.{Actor, ActorLogging}

trait MopeActor
  extends Actor
  with UnhandledReceiver
  with ActorLogging {

  override def preStart(): Unit = log.debug("starting")
  override def postStop(): Unit = log.debug("stopped")
}
