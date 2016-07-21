package de.thm.moie.server

import akka.pattern.pipe
import akka.actor.{Actor, ActorLogging}
import de.thm.moie.compiler.DocumentationLike
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.concurrent.Future

class DocumentationProvider(docLike: DocumentationLike)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import context.dispatcher
  import DocumentationProvider._

  override def handleMsg: Receive = {
    case GetDocumentation(className) =>
      Future {
        docLike.getDocumentation(className)
      } pipeTo sender
  }
}

object DocumentationProvider {
  case class GetDocumentation(className:String)
}