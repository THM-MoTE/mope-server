package de.thm.moie.server

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import de.thm.moie.compiler.JumpToLike
import de.thm.moie.project.{DeclarationRequest, FilePath}
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.concurrent.Future

class JumpToProvider(jumpLike:JumpToLike)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import context.dispatcher

  override def handleMsg: Receive = {
    case DeclarationRequest(className) =>
      Future {
        val file = jumpLike.getSrcFile(className).map(FilePath.apply)
        log.debug("src file {}", file)
        file
      } pipeTo sender
  }
}
