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

  override def handleMsg: Receive = {
    case CompletionRequest(_,_,word) if word.isEmpty =>
      //ignore empty strings
      sender ! Set.empty[CompletionResponse]
    case CompletionRequest(_,_,word) if word.endsWith(".") =>
      //searching for a class inside another class
      containingPackages(word.dropRight(1)).run().
      map(logSuggestions(word)) pipeTo sender
    case CompletionRequest(filename,FilePosition(line,_),word) =>
      //searching for a possible not-completed class
      closestKeyWordType(word).
        mapConcat(x => x).
        merge(findMatchingClasses(word).mapConcat(x => x)).
        merge(localVariables(filename, word, line)).
        via(toSet).
        toMat(Sink.head)(Keep.right).run().
        map(logSuggestions(word)) pipeTo sender
  }

  private def toSet[A]: Flow[A, Set[A], NotUsed] =
    Flow[A].fold(Set.empty[A]) {
      case (acc, elem) => acc + elem
    }

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
        CompletionResponse(tpe, name, paramOpt, classComment)
    }


/** Returns all components/packages inside of `word`. */
  private def containingPackages(word:String): RunnableGraph[Future[Set[CompletionResponse]]] =
    Source.fromFuture(compiler.getClassesAsync(word)).
      mapConcat[(String, CompletionResponse.CompletionType.Value)](x => x).
      via(withParameters).
      via(toCompletionResponse).
      via(toSet).
      toMat(Sink.head)(Keep.right)

  /** Finds the keywords, types that starts with `word`. */
  private def closestKeyWordType(word:String): Source[Set[CompletionResponse], NotUsed] =
    Source.fromFuture(findClosestMatch(word, keywords ++ types)).
      mapConcat(x => x).
      map { x =>
        if (keywords.contains(x))
          CompletionResponse(CompletionType.Keyword, x, None, None)
        else if (types.contains(x))
          CompletionResponse(CompletionType.Type, x, None, None)
        else {
          log.warning("Couldn't find CompletionType for {}", x)
          CompletionResponse(CompletionType.Keyword, x, None, None)
        }
      }.
      via(toSet)

  private def findMatchingClasses(word:String): Source[Set[CompletionResponse], NotUsed] = {
    val pointIdx = word.lastIndexOf(".")
    if(pointIdx == -1) toCompletionResponse(word, Future(compiler.getGlobalScope()))
    else {
      val parentPackage = word.substring(0, pointIdx)
      toCompletionResponse(word, compiler.getClassesAsync(parentPackage))
    }
  }

  private def toCompletionResponse(word:String, xs:Future[Set[(String, CompletionType.Value)]]): Source[Set[CompletionResponse], NotUsed] =
    Source.fromFuture(xs).
      mapAsync(2) { clazzes =>
        val classMap = clazzes.toMap
        val classNames = clazzes.map(_._1)
        findClosestMatch(word, classNames).map { set =>
          set.map { clazz => clazz -> classMap(clazz)
          }
        }
      }.
      mapConcat[(String,CompletionType.Value)](x => x).
      via(withParameters).
      via(toCompletionResponse).
      via(toSet)

  private def localVariables(filename:String, word:String, lineNo:Int): Source[CompletionResponse, _] = {
    val path = Paths.get(filename)
    val nameStartsWith =
      Flow[(String, String)].filter {
        case (_, name) => name.startsWith(word)
      }
    val complResponse =
      Flow[(String,String)].map {
        case (tpe, name) =>
          CompletionResponse(CompletionType.Variable, name, None, None)
      }

    val possibleLines = FileIO.fromPath(path).
      via(Framing.delimiter(ByteString("\n"), 8192, true)).
      map(_.utf8String).
      take(lineNo)

    possibleLines.
      via(onlyVariables).
      via(nameStartsWith).
      via(complResponse)
  }

  val ignoredModifiers =
    "(?:" + List("(?:parameter)",
      "(?:discrete)",
      "(?:input)",
      "(?:output)",
      "(?:flow)").mkString("|") + ")"
  val typeRegex = """(\w[\w\-\_\.]*)"""
  val identRegex = """(\w[\w\-\_]*)"""

  val variableRegex =
    s"""\\s*(?:$ignoredModifiers\\s+)?$typeRegex\\s+$identRegex.*""".r

  private def onlyVariables =
    Flow[String].collect {
      case variableRegex(_, tpe, name) => (tpe, name)
      case variableRegex(tpe, name) => (tpe, name)
    }

  private def modelLines: Flow[String, String, NotUsed] =
    Flow[String].filter { line =>
      val matcher = ScriptingHelper.modelPattern.matcher(line)
      matcher.find()
    }

  /** Removes all entries from `words` which doesn't start with `word` */
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
