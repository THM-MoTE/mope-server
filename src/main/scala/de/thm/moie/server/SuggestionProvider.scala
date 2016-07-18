package de.thm.moie.server

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._
import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import de.thm.moie.Global
import de.thm.moie.compiler.CompletionLike
import de.thm.moie.project.CompletionResponse.CompletionType
import de.thm.moie.project.{CompletionRequest, CompletionResponse}
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.concurrent.Future

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

  override def preStart(): Unit = {
    log.debug("started")
  }

  override def handleMsg: Receive = {
    case CompletionRequest(_,_,word) if word.isEmpty =>
      //ignore empty strings
      sender ! Set.empty[CompletionResponse]
    case CompletionRequest(_,_,word) if word.endsWith(".") =>
      containingPackages(word.dropRight(1)).run() pipeTo sender
    case CompletionRequest(_,_,word) =>
      closestKeyWordType(word).
        mapConcat(x => x).
        merge(findMatchingClasses(word).mapConcat(x => x)).
        via(toSet).
        toMat(Sink.head)(Keep.right).run() pipeTo sender
  }

  private def toSet[A]: Flow[A, Set[A], NotUsed] =
    Flow[A].fold(Set.empty[A]) {
      case (acc, elem) => acc + elem
    }

  private def withParameters: Flow[(String, CompletionResponse.CompletionType.Value), (String, CompletionResponse.CompletionType.Value, List[String]), NotUsed] =
    Flow[(String, CompletionResponse.CompletionType.Value)].map {
      case (name, tpe) =>
        val params = compiler.getParameters(name).map {
          case (name, Some(tpe)) => tpe+", "+name
          case (name, None) => name
        }
        (name, tpe, params)
    }

  private def toCompletionResponse: Flow[(String, CompletionResponse.CompletionType.Value, List[String]), CompletionResponse, NotUsed] =
    Flow[(String, CompletionResponse.CompletionType.Value, List[String])].map {
      case (name, tpe, parameters) =>
        val paramOpt = if(parameters.isEmpty) None else Some(parameters)
        val classComment = compiler.getClassDocumentation(name)
        log.debug("{} params: {}, comment: {}", name, parameters, classComment)
        CompletionResponse(tpe, name, paramOpt, classComment)
    }

  private def containingPackages(word:String): RunnableGraph[Future[Set[CompletionResponse]]] =
    Source.fromFuture(compiler.getClassesAsync(word)).
      mapConcat[(String, CompletionResponse.CompletionType.Value)](x => x).
      via(withParameters).
      via(toCompletionResponse).
      via(toSet).
      toMat(Sink.head)(Keep.right)

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
          val xs = set.map { clazz =>
            val classComment = compiler.getClassDocumentation(clazz)
            //TODO find parameters
            //rewrite using withParameters
            CompletionResponse(classMap(clazz), clazz, None, classComment)
          }
          log.debug("final suggestions: {}", xs)
          xs
        }
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
