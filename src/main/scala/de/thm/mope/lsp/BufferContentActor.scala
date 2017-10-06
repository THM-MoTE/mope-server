package de.thm.mope.lsp

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import java.nio.file.Path

class BufferContentActor
    extends Actor
    with ActorLogging {
  import BufferContentActor._

  var currentContent:BufferContent = null

  override def receive:Receive = {
    case x:BufferContent =>
      currentContent = x
      context.become(initialized)
  }

  private def initialized:Receive = {
    case GetContentRange(Some(range)) =>
      val containingLines = currentContent.lines.slice(range.start.line,range.end.line+1)
      val firstLine = containingLines.head.drop(range.start.character)
      val lastLine = containingLines.last.take(range.end.character)
      sender ! ((firstLine :: containingLines.tail.init) :+ lastLine).mkString("\n")
    case GetContentRange(None) => sender ! currentContent.content
  }
}

object BufferContentActor {
  case class BufferContent(file:Path, content:String) {
    lazy val lines:List[String] = content.split('\n').toList
  }
  case class GetContentRange(range:Option[messages.Range]=None)
}

