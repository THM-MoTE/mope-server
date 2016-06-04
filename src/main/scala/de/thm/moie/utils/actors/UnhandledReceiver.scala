/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.utils.actors

import akka.actor.Actor
import de.thm.moie.server.LogMessages

trait UnhandledReceiver {
  this: Actor with LogMessages =>

  private val actorName = this.self.path.name

  def catchUnhandledMsgs: Actor.Receive = {
    case a:Any => log.warning(s"can't handle $a")
  }

  def handleMsg: Receive

  override def receive: Receive = handleMsg.orElse(catchUnhandledMsgs)
}
