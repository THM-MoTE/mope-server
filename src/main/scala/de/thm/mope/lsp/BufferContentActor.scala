package de.thm.mope.lsp

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import java.nio.file.Path

import de.thm.mope.lsp.messages.Position

class BufferContentActor
    extends Actor
    with ActorLogging {
  import BufferContentActor._

  /* TODO:
   - hold a map of all opened files Path -> Content, keep it up-to-date
   - use this map to query infos, e.g.: word of goToDefinition
     - if map for path empty: use file from disk
   */

  var currentContent:BufferContent = null

  val wordPattern = """[\w\d\.\-]+""".r

  override def receive:Receive = {
    case x:BufferContent =>
      currentContent = x
      log.debug("Initialized content: {}", currentContent.content)
      context.become(initialized)
  }

  private def initialized:Receive = {
    case x:BufferContent =>
      currentContent = x
      log.debug("New content: {}", currentContent.content)
    case GetContentRange(Some(range)) =>
      val containingLines = currentContent.lines.slice(range.start.line,range.end.line+1)
      val firstLine = containingLines.head.drop(range.start.character)
      val lastLine = containingLines.last.take(range.end.character)
      val content = ((firstLine :: containingLines.tail.init) :+ lastLine).mkString("\n")
      log.debug("Content-Range: {}", content)
      sender ! content
    case GetContentRange(None) => sender ! currentContent.content
    case GetWord(Position(lineIdx,charIdx)) =>
      log.debug("matches: {}", wordPattern.findAllIn(currentContent.lines(lineIdx)).toList)
      val word = wordPattern
        .findAllIn(currentContent.lines(lineIdx)).matchData
        .find { m => m.start <= charIdx && m.end>=charIdx }
        .map { m => m.toString }
      sender ! word
  }
}

object BufferContentActor {
  case class BufferContent(file:Path, content:String) {
    lazy val lines:List[String] = content.split('\n').toList
  }
  case class GetContentRange(range:Option[messages.Range]=None)
  case class GetWord(pos:Position)
}

