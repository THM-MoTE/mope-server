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
        merge(findMatchingClasses(word)).
        merge(localVariables(filename, word, line)).
        toMat(toStartsWith(word))(Keep.right).run().
        map(logSuggestions(word)) pipeTo sender
    case TypeRequest(filename, FilePosition(line, _), word) =>
      typeOf(filename, word, line).
        toMat(Sink.headOption)(Keep.right).run().
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
        CompletionResponse(tpe, name, paramOpt, classComment)
    }


/** Returns all components/packages inside of `word`. */
  private def containingPackages(word:String): RunnableGraph[Future[Set[CompletionResponse]]] =
    Source.fromFuture(compiler.getClassesAsync(word)).
      mapConcat(identity).
      via(withParameters).
      via(toCompletionResponse).
      toMat(toStartsWith(word))(Keep.right)


  /** Finds the keywords, types that starts with `word`. */
  private def closestKeyWordType(word:String): Source[CompletionResponse, NotUsed] =
    Source(keywords ++ types).
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
      via(onlyStartsWith(word))

  private def findMatchingClasses(word:String): Source[CompletionResponse, NotUsed] = {
    val pointIdx = word.lastIndexOf(".")
    if(pointIdx == -1) toCompletionResponse(word, Future(compiler.getGlobalScope()))
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
    val path = Paths.get(filename)
    val nameStartsWith =
      Flow[(String, String, Option[String])].filter {
        case (_, name, _) => name.startsWith(word)
      }
    val complResponse =
      Flow[(String,String, Option[String])].map {
        case (tpe, name, commentOpt) =>
          CompletionResponse(CompletionType.Variable, name, None, commentOpt)
      }

    val possibleLines = lines(filename).take(lineNo)

    possibleLines.
      via(onlyVariables).
      via(nameStartsWith).
      via(complResponse)
  }

  private def onlyVariables =
    Flow[String].collect {
      case variableCommentRegex(tpe,name,comment) => (tpe, name, Some(comment))
      case variableRegex(tpe, name) => (tpe, name, None)
    }

  private def modelLines: Flow[String, String, NotUsed] =
    Flow[String].filter { line =>
      val matcher = ScriptingHelper.modelPattern.matcher(line)
      matcher.find()
    }

  def onlyStartsWith(word:String) =
    Flow[CompletionResponse].filter { response =>
      response.name.startsWith(word)
    }
}
