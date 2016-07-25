package de.thm.moie.declaration

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import de.thm.moie.position.FilePath
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
        log.info("src of {} is {}", className, file)
        file
      } pipeTo sender
  }
}
