package de.thm.moie.server

import java.nio.file.Path

import akka.pattern.pipe
import akka.actor.Actor
import de.thm.moie.Global
import de.thm.moie.project.Completion
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.collection.Seq
import scala.concurrent.Future

class CodeCompletionActor
  extends Actor
    with UnhandledReceiver
    with LogMessages {

  import context.dispatcher

  def filterLines(line:String):Boolean = !line.isEmpty

  val keywords =
    Global.readValuesFromResource(getClass.getResource("/completion/keywords.conf").toURI.toURL)(filterLines _)
  val types =
    Global.readValuesFromResource(getClass.getResource("/completion/types.conf").toURI.toURL)(filterLines _)

  override def handleMsg: Receive = {
    case Completion(_,_,word) => findClosestMatch(word) pipeTo sender
  }

  def findClosestMatch(word:String): Future[Seq[String]]= Future {
    @annotation.tailrec
    def closestMatch(w:String, remainingWords:Seq[String], idx:Int): Seq[String] =
      if(w.length > 0) {
        val char = w.head
        val filtered = remainingWords.filter(_.charAt(idx) == char)
        closestMatch(w.tail, filtered, idx+1)
      } else remainingWords

    closestMatch(word, keywords ++ types, 0)
  }
}