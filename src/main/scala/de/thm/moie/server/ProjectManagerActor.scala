package de.thm.moie.server

import akka.pattern.pipe
import akka.actor.{Actor, ActorLogging, ActorRef}
import de.thm.moie.project.ProjectDescription
import de.thm.moie.utils.actors.UnhandledReceiver

class ProjectManagerActor(description:ProjectDescription)
  extends Actor
  with LogMessages
  with UnhandledReceiver {
  override def handleMsg: Receive = {
    case s:String => println("not implemented yet!")
  }

  override def postStop(): Unit = {
    log.info("stopping")
  }
}
