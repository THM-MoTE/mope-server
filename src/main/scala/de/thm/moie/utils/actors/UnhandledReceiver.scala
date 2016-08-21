/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.utils.actors

import akka.actor.{Actor, ActorLogging}

trait UnhandledReceiver {
  this: Actor with ActorLogging =>

  override def unhandled(msg:Any): Unit =
    log.warning("Didn't handle message: `{}`", msg)
}
