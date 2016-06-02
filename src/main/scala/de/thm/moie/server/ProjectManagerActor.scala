package de.thm.moie.server

import akka.pattern.pipe
import akka.actor.{Actor, ActorLogging, ActorRef}
import de.thm.moie.compiler.{AsyncModelicaCompiler, ModelicaCompiler}
import de.thm.moie.project.ProjectDescription
import de.thm.moie.utils.actors.UnhandledReceiver

class ProjectManagerActor(description:ProjectDescription,
                          compiler:ModelicaCompiler with AsyncModelicaCompiler)
  extends Actor
  with UnhandledReceiver
  with LogMessages {

  override def handleMsg: Receive = {
    case s:String => println("not implemented yet!")
  }

  override def postStop(): Unit = {
    log.info("stopping")
  }
}
