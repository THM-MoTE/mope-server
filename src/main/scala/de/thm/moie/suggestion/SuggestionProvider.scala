/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.moie.suggestion

import java.nio.file.{Files, Paths}

import akka.NotUsed
import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import de.thm.moie.Global
import de.thm.moie.position.FilePosition
import de.thm.moie.suggestion.CompletionResponse.CompletionType
import de.thm.moie.utils.actors.UnhandledReceiver
import omc.corba.ScriptingHelper

import scala.concurrent.Future

/** An Actor which provides suggestions (code completions) for a given word. */
class SuggestionProvider(compiler:CompletionLike)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import context.dispatcher
  implicit val mat = ActorMaterializer(namePrefix = Some("suggestion-stream"))

  def filterLines(line:String):Boolean = !line.isEmpty

  val ignoredModifiers =
    "(?:" + List("(?:parameter)",
      "(?:discrete)",
      "(?:input)",
      "(?:output)",
      "(?:flow)").mkString("|") + ")"
  val typeRegex = """(\w[\w\-\_\.]*)"""
  val identRegex = """(\w[\w\-\_]*)"""
  val commentRegex = """"([^"]+)";"""

  val variableRegex =
    s"""\\s*(?:$ignoredModifiers\\s+)?$typeRegex\\s+$identRegex.*""".r
  val variableCommentRegex =
    s"""\\s*(?:$ignoredModifiers\\s+)?$typeRegex\\s+$identRegex.*\\s+$commentRegex""".r


  val keywords =
    Global.readValuesFromResource(
        getClass.getResource("/completion/keywords.conf").toURI.toURL)(filterLines _).toSet
  val types =
    Global.readValuesFromResource(
        getClass.getResource("/completion/types.conf").toURI.toURL)(filterLines _).toSet

  val logSuggestions: String => Set[CompletionResponse] => Set[CompletionResponse] = { word => suggestions =>
    if(log.isDebugEnabled) log.debug("suggestions for {} are {}", word, suggestions)
    else log.info("found {} suggestion(s) for {}", suggestions.size, word)
    suggestions
  }

  val logType: String => Option[TypeOf] => Option[TypeOf] = { name => tpe =>
    log.info("Type of {} is {}", name, if(tpe.isDefined) tpe.get.`type` else "unknown")
    tpe
  }

  override def receive: Receive = {
    case CompletionRequest(_,_,word) if word.isEmpty =>
      //ignore empty strings
      sender ! Set.empty[CompletionResponse]
    case CompletionRequest(_,_,word) if word.endsWith(".") =>
      //searching for a class inside another class
      containingPackages(word.dropRight(1)).
        toMat(toSet)(Keep.right).
        run().
        map(logSuggestions(word)) pipeTo sender
    case CompletionRequest(filename,FilePosition(line,_),word) =>
      //searching for a possible not-completed class
      closestKeyWordType(word).
        merge(findMatchingClasses(word)).
        merge(localVariables(filename, word, line)).
        merge(memberAccess(filename, word, line)).
        toMat(toStartsWith(word))(Keep.right).
        run().
        map(logSuggestions(word)) pipeTo sender
    case TypeRequest(filename, FilePosition(line, _), word) =>
      typeOf(filename, word, line).
        toMat(Sink.headOption)(Keep.right).
        run().
        map(logType(word)) pipeTo sender
  }

  private def toSet[A] =
    Sink.fold[Set[A], A](Set[A]()) {
      case (set, elem) => set + elem
    }

  private def toStartsWith(word:String) = onlyStartsWith(word).toMat(toSet)(Keep.right)

  /** Adds to the given tupel of (className, classType) a list of parameters. */
  private def withParameters: Flow[(String, CompletionResponse.CompletionType.Value), (String, CompletionResponse.CompletionType.Value, List[String]), NotUsed] =
    Flow[(String, CompletionResponse.CompletionType.Value)].map {
      case (name, tpe) =>
        val params = compiler.getParameters(name).map {
          case (name, Some(tpe)) => tpe+" "+name
          case (name, None) => name
        }
        (name, tpe, params)
    }

  /** Converts the given tripel of (className, classType, parameterlist) into a CompletionResponse. */
  private def toCompletionResponse: Flow[(String, CompletionType.Value, List[String]), CompletionResponse, NotUsed] =
    Flow[(String, CompletionType.Value, List[String])].map {
      case (name, tpe, parameters) =>
        val paramOpt = if(parameters.isEmpty) None else Some(parameters)
        val classComment = compiler.getClassDocumentation(name)
        CompletionResponse(tpe, name, paramOpt, classComment, None)
    }


/** Returns all components/packages inside of `word`. */
  private def containingPackages(word:String): Source[CompletionResponse, NotUsed] =
    Source.fromFuture(compiler.getClassesAsync(word)).
      mapConcat(identity).
      via(withParameters).
      via(toCompletionResponse)


  /** Finds the keywords, types that starts with `word`. */
  private def closestKeyWordType(word:String): Source[CompletionResponse, NotUsed] =
    Source(keywords ++ types).
      map { x =>
        if (keywords.contains(x))
          CompletionResponse(CompletionType.Keyword, x, None, None, None)
        else if (types.contains(x))
          CompletionResponse(CompletionType.Type, x, None, None, None)
        else {
          log.warning("Couldn't find CompletionType for {}", x)
          CompletionResponse(CompletionType.Keyword, x, None, None, None)
        }
      }

  private def findMatchingClasses(word:String): Source[CompletionResponse, NotUsed] = {
    val pointIdx = word.lastIndexOf(".")
    if(pointIdx == -1)
      toCompletionResponse(word, Future(compiler.getGlobalScope()))
    else {
      val parentPackage = word.substring(0, pointIdx)
      toCompletionResponse(word, compiler.getClassesAsync(parentPackage))
    }
  }

  private def toCompletionResponse(word:String, xs:Future[Set[(String, CompletionType.Value)]]): Source[CompletionResponse, NotUsed] =
    Source.fromFuture(xs).
      mapConcat(identity).
      via(withParameters).
      via(toCompletionResponse)

  private def lines(file:String) =
    FileIO.fromPath(Paths.get(file)).
      via(Framing.delimiter(ByteString("\n"), 8192, true)).
      map(_.utf8String)

  private def nameEquals(word:String) =
    Flow[(String, String, Option[String])].filter {
      case (_, name, _) => name == word
    }

  private def typeOf(filename:String, word:String, lineNo:Int): Source[TypeOf, _] = {
    val toTypeOf =
      Flow[(String, String, Option[String])].map {
        case (tpe, name, comment) => TypeOf(name, tpe, comment)
      }

    identRegex.r.
      findFirstIn(word).
      map { ident =>
        lines(filename).
          take(lineNo).
          via(onlyVariables).
          via(nameEquals(ident)).
          via(toTypeOf)
      }.getOrElse(Source.empty)
  }

  private def localVariables(filename:String, word:String, lineNo:Int): Source[CompletionResponse, _] = {
    val possibleLines = lines(filename).take(lineNo)
    possibleLines.
      via(onlyVariables).
      via(complResponse)
  }

  def memberAccess(filename:String, word:String, lineNo:Int) = {
    val srcFile =
      Flow[TypeOf].map { tpe =>
        if(!types.contains(tpe.`type`) || keywords.contains(tpe.`type`)) compiler.getSrcFile(tpe.`type`)
        else None
      }.collect { case Some(file) => file }

    val pointIdx = word.lastIndexOf(".")
    if(pointIdx == -1) Source.empty
    else {
      val objectName = word.substring(0, pointIdx)
      typeOf(filename, objectName, lineNo).
        via(srcFile).
        flatMapConcat(lines).
        via(onlyVariables).
        via(propertyResponse).
        map { resp =>
          resp.copy(name = objectName+"."+resp.name)
        }
    }
  }

  private def onlyVariables =
    Flow[String].collect {
      case variableCommentRegex(tpe,name,comment) => (tpe, name, Some(comment))
      case variableRegex(tpe, name) => (tpe, name, None)
    }

  val complResponse =
    Flow[(String,String, Option[String])].map {
      case (tpe, name, commentOpt) =>
        val pointIdx = tpe.lastIndexOf('.')
        val shortenedType = if(pointIdx != -1) tpe.substring(pointIdx+1) else tpe
        CompletionResponse(CompletionType.Variable, name, None, commentOpt, Some(shortenedType))
    }

  val propertyResponse =
    complResponse.map(x => x.copy(completionType = CompletionType.Property))

  def onlyStartsWith(word:String) =
    Flow[CompletionResponse].filter { response =>
      response.name.startsWith(word)
    }
}
