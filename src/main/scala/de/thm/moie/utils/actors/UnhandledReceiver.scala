/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.utils.actors

import akka.actor.{Actor, ActorLogging}

trait UnhandledReceiver {
  this: Actor with ActorLogging =>

  private val actorName = this.self.path.name

  def catchUnhandledMsgs: Actor.Receive = {
    case a:Any => log.warning("can't handle {}", a)
  }

  def handleMsg: Receive

  override def receive: Receive = handleMsg.orElse(catchUnhandledMsgs)
}
