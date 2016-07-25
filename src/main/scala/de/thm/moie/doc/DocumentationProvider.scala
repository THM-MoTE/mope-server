package de.thm.moie.doc

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.concurrent.Future

class DocumentationProvider(docLike: DocumentationLike)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import DocumentationProvider._
  import context.dispatcher

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