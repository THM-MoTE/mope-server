package de.thm.mope.lsp

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import java.nio.file.Path
import scala.collection.mutable
import de.thm.mope.lsp.messages.Position

class BufferContentActor
    extends Actor
    with ActorLogging {
  import BufferContentActor._

  /* TODO:
   - hold a map of all opened files Path -> Content, keep it up-to-date
   - use this map to query infos, e.g.: word of goToDefinition
     - if map for path empty: use file from disk
   - unload after user saved a file
   */

  val fileContents = mutable.Map.empty[Path, BufferContent]

  val wordPattern = """[\w\d\.\-]+""".r

  override def receive:Receive = {
    case x:BufferContent =>
      fileContents += x.file -> x
      log.debug("Initialized for file: {}", x.file)
      context.become(initialized)
  }

  private def initialized:Receive = {
    case x:BufferContent =>
      fileContents += x.file -> x
      log.debug("New content for: {}", x.file)
    case GetContentRange(file, Some(range)) =>
      val lines = fileContents.get(file).map(_.lines).get //FIXME: handle non cached files
      val containingLines = lines.slice(range.start.line,range.end.line+1)
      val firstLine = containingLines.head.drop(range.start.character)
      val lastLine = containingLines.last.take(range.end.character)
      val content = ((firstLine :: containingLines.tail.init) :+ lastLine).mkString("\n")
      log.debug("Content-Range: {}", content)
      sender ! content
    case GetContentRange(file, None) => sender ! fileContents.get(file).map(_.content)
    case GetWord(file, Position(lineIdx,charIdx)) =>
      val content = fileContents.get(file).map(_.lines).get //FIXME: handle non cached files

      val word = wordPattern
        .findAllMatchIn(content(lineIdx))
        .find{ m => m.start <= charIdx && m.end>=charIdx }
        .map{ m => m.toString }
      log.debug("found word: {}", word)
      sender ! word
  }
}

object BufferContentActor {
  case class BufferContent(file:Path, content:String) {
    lazy val lines:List[String] = content.lines.toList
  }
  case class GetContentRange(file:Path, range:Option[messages.Range]=None)
  case class GetWord(file:Path, pos:Position)
}

