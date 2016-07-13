package de.thm.moie.server

import java.nio.file.Path

import akka.pattern.pipe
import akka.actor.Actor
import de.thm.moie.Global
import de.thm.moie.compiler.ModelicaCompiler
import de.thm.moie.project.CompletionResponse.CompletionType
import de.thm.moie.project.{CompletionRequest, CompletionResponse}
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.collection.Seq
import scala.concurrent.Future

class CodeCompletionActor(compiler:ModelicaCompiler)
  extends Actor
    with UnhandledReceiver
    with LogMessages {

  import context.dispatcher

  def filterLines(line:String):Boolean = !line.isEmpty

  val keywords =
    Global.readValuesFromResource(
        getClass.getResource("/completion/keywords.conf").toURI.toURL)(filterLines _).toSet
  val types =
    Global.readValuesFromResource(
        getClass.getResource("/completion/types.conf").toURI.toURL)(filterLines _).toSet

  override def handleMsg: Receive = {
    case CompletionRequest(_,_,word) if word.endsWith(".") =>
      log.debug("word {}", word)
      compiler.getClassesAsync(word.dropRight(1)).
      map { set =>
        set.map { case (name, tpe) =>
          CompletionResponse(tpe, name, None)
        }
      } pipeTo sender
    case CompletionRequest(_,_,word) =>
      findClosestMatch(word, keywords ++ types).map { set =>
        set.map { x =>
          if(keywords.contains(x))
            CompletionResponse(CompletionType.Keyword, x, None)
          else if(types.contains(x))
            CompletionResponse(CompletionType.Type, x, None)
          else {
            CompletionResponse(CompletionType.Keyword, x, None)
            log.warning("Couldn't find CompletionType for {}", x)
          }
        }
      } pipeTo sender
  }

  def findClosestMatch(word:String, words:Set[String]): Future[Set[String]]= Future {
    @annotation.tailrec
    def closestMatch(w:String, remainingWords:Set[String], idx:Int): Set[String] =
      if(w.length > 0) {
        val char = w.head
        val filtered = remainingWords.filter(_.charAt(idx) == char)
        closestMatch(w.tail, filtered, idx+1)
      } else remainingWords

    closestMatch(word, words, 0)
  }
}
