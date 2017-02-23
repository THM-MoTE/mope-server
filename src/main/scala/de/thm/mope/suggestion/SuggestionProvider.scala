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

package de.thm.mope.suggestion

import java.nio.file.{Files, Paths}

import akka.NotUsed
import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import de.thm.mope.Global
import de.thm.mope.position.FilePosition
import de.thm.mope.suggestion.Suggestion.Kind
import de.thm.mope.utils.actors.UnhandledReceiver
import omc.corba.ScriptingHelper

import scala.concurrent.Future

/** An Actor which provides suggestions (code completions) for a given word. */
class SuggestionProvider(compiler:CompletionLike)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import context.dispatcher
  implicit val mat = ActorMaterializer(namePrefix = Some("suggestion-stream"))

  val keywords =
    Global.readValuesFromResource(
        getClass.getResource("/completion/keywords.conf").toURI.toURL)(SrcFileInspector.nonEmptyLines).toSet
  val types =
    Global.readValuesFromResource(
        getClass.getResource("/completion/types.conf").toURI.toURL)(SrcFileInspector.nonEmptyLines).toSet

  val logSuggestions: String => Set[Suggestion] => Set[Suggestion] = { word => suggestions =>
    if(log.isDebugEnabled) log.debug("suggestions for {} are {}", word, suggestions.map(_.displayString))
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
      sender ! Set.empty[Suggestion]
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
        merge(localVariables(filename, line, word)).
        //merge(memberAccess(filename, word, line)).
        toMat(toStartsWith(word))(Keep.right).
        run().
        map(logSuggestions(word)) pipeTo sender
    case TypeRequest(filename, FilePosition(line, _), word) =>
      new SrcFileInspector(Paths.get(filename)).typeOf(word, line).
        toMat(Sink.headOption)(Keep.right).
        run().
        map(logType(word)) pipeTo sender
  }

  private def toSet[A] =
    Sink.fold[Set[A], A](Set[A]()) {
      case (set, elem) => set + elem
    }

  private def toStartsWith(word:String) = (new PrefixMatcher(word)).startsWith.toMat(toSet)(Keep.right)

  /** Adds to the given tupel of (className, classType) - returned from CompletionLike#getClasse - a list of parameters. */
  private def withParameters: Flow[(String, Suggestion.Kind.Value), (String, Suggestion.Kind.Value, List[String]), NotUsed] =
    Flow[(String, Suggestion.Kind.Value)].map {
      case (name, tpe) =>
        val params = compiler.getParameters(name).map {
          case (name, Some(tpe)) => tpe+" "+name
          case (name, None) => name
        }
        (name, tpe, params)
    }

  /** Converts the given tripel of (className, completionType, parameterlist) into a CompletionResponse. */
  private def toCompletionResponse: Flow[(String, Kind.Value, List[String]), Suggestion, NotUsed] =
    Flow[(String, Kind.Value, List[String])].map {
      case (name, tpe, parameters) =>
        val paramOpt = if(parameters.isEmpty) None else Some(parameters)
        val classComment = compiler.getClassDocumentation(name)
        Suggestion(tpe, name, paramOpt, classComment, None)
    }


  private def localVariables(filename:String, lineNo:Int, word:String):Source[Suggestion,_] =
    (new SrcFileInspector(Paths.get(filename)))
      .localVariables(lineNo)
      .map { variable =>
        Suggestion(Kind.Variable, variable.name, None, variable.docString, Some(variable.`type`))
      }

/** Returns all components/packages inside of `word`. */
  private def containingPackages(word:String): Source[Suggestion, NotUsed] =
    Source.fromFuture(compiler.getClassesAsync(word)).
      mapConcat(identity).
      via(withParameters).
      via(toCompletionResponse)


  /** Finds the keywords, types that starts with `word`. */
  private def closestKeyWordType(word:String): Source[Suggestion, NotUsed] =
    Source(keywords ++ types).
      map { x =>
        if (keywords.contains(x))
          Suggestion(Kind.Keyword, x, None, None, None)
        else if (types.contains(x))
          Suggestion(Kind.Type, x, None, None, None)
        else {
          log.warning("Couldn't find CompletionType for {}", x)
          Suggestion(Kind.Keyword, x, None, None, None)
        }
      }

  private def findMatchingClasses(word:String): Source[Suggestion, NotUsed] = {
    val (parent, tpe) = sliceAtLastDot(word)
    if(tpe.isEmpty)
      toCompletionResponse(word, Future(compiler.getGlobalScope()))
    else
      toCompletionResponse(word, compiler.getClassesAsync(parent))
  }

  private def toCompletionResponse(word:String, xs:Future[Set[(String, Kind.Value)]]): Source[Suggestion, NotUsed] =
    Source.fromFuture(xs).
      mapConcat(identity).
      via(withParameters).
      via(toCompletionResponse)

  def memberAccess(filename:String, word:String, lineNo:Int) = {
    val srcFile =
      Flow[TypeOf].map { tpe =>
        if(!types.contains(tpe.`type`) || keywords.contains(tpe.`type`)) compiler.getSrcFile(tpe.`type`)
        else None
      }.collect { case Some(file) => file }

    val pointIdx = word.lastIndexOf(".")
    val (objectName, member) = sliceAtLastDot(word)
    if(member.isEmpty) Source.empty
    else {
      //TODO fix this
      Source.empty
      /*
      typeOf(filename, objectName, lineNo).
        via(srcFile).
        flatMapConcat(lines).
        via(onlyVariables).
        via(propertyResponse).
        map { resp =>
          resp.copy(name = objectName+"."+resp.name)
        }
        */
    }
  }

  val variableResponse =
    Flow[(String,String, Option[String])].map {
      case (tpe, name, commentOpt) =>
        val pointIdx = tpe.lastIndexOf('.')
        val shortenedType = if(pointIdx != -1) tpe.substring(pointIdx+1) else tpe
        Suggestion(Kind.Variable, name, None, commentOpt, Some(shortenedType))
    }

  val propertyResponse =
    variableResponse.map(x => x.copy(kind = Kind.Property))
}
