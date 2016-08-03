package de.thm.moie.doc

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.concurrent.Future

/*+ An Actor which returns the documentation (as DocInfo) of a given `className`. */
class DocumentationProvider(docLike: DocumentationLike)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import DocumentationProvider._
  import context.dispatcher

  override def handleMsg: Receive = {
    case GetDocumentation(className) =>
      Future {
        val docOpt = docLike.getDocumentation(className)
        log.info(
            if(docOpt.isDefined) "got documentation for {}"
            else "no documentation for {}",
            className)
        docOpt
      } pipeTo sender
  }
}

object DocumentationProvider {
  case class GetDocumentation(className:String)
}
