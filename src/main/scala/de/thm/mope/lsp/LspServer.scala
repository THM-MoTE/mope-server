package de.thm.mope.lsp

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.stream._
import akka.util.ByteString
import akka.stream.scaladsl._
import de.thm.mope.lsp.messages._
import spray.json._
import de.thm.mope.server.JsonSupport
import de.thm.mope.utils.StreamUtils

import scala.util._

class LspServer(implicit val system:ActorSystem)
    extends JsonSupport
      with LspJsonSupport {

  implicit val log = Logging(system, getClass)
  import system.dispatcher
  val parallelism = 4
  val notificationBufferSize = 8024

  def handleError(ex:Throwable):ResponseError = ex match {
    case ex:DeserializationException =>
      log.debug("got serial error {}", ex)
      ResponseError(ErrorCodes.ParseError, ex.getMessage)
    case ex:MethodNotFoundException =>
      log.debug("got notfound error {}", ex)
      ResponseError(ErrorCodes.MethodNotFound, ex.getMessage)
    case ex:InitializeException =>
      log.debug("initialization failure {}", ex)
      ResponseError(ErrorCodes.ServerNotInitialized, ex.getMessage)
    case ex =>
      log.error("Unhandled exception", ex)
      ResponseError(ErrorCodes.InternalError, ex.getMessage)
  }

  def connectTo[I:JsonFormat,O:JsonFormat](userHandlers:RpcMethod[I,O]):Flow[ByteString,ByteString,ActorRef] = {
    val handlers = StreamUtils.broadcastAll(userHandlers.toFlows(parallelism))
    val methods = userHandlers.methods
    log.debug("Available methods: {}", methods)

    val notificationSource:Source[String, ActorRef] =
      Source.actorRef[NotificationMessage](notificationBufferSize, OverflowStrategy.dropHead)
      .map(_.toJson.toString)

    Flow[ByteString]
      .via(new ProtocolHandler())
      .map { s => s.parseJson.convertTo[RpcMessage] }
      .log("in")
      .flatMapConcat { msg =>
        val responseMessage =
          Flow[Try[JsValue]]
            .map(_ ->msg) //zip with message
            .collect { case (tryV, RequestMessage(id,_,_,_)) =>
              //don't create a resposne if it's a notification
              tryV match {
                case Failure(ex) => ResponseMessage(id,None, Some(handleError(ex))).toJson.toString
                case Success(value) => ResponseMessage(id,Some(value), None).toJson.toString
              }
            }

        if(!methods.contains(msg.method)){
          Source.single(Failure(MethodNotFoundException(s"Method '${msg.method}' not found"))).via(responseMessage)
        } else {
          Source.single(msg)
            .via(handlers)
            .log("out")
            .via(responseMessage)
        }
      }
      .mergeMat(notificationSource, true)(Keep.right)
      .map { payloadStr =>
        ByteString(s"""Content-Length: ${payloadStr.length}\r
           |\r
           |$payloadStr""".stripMargin)
      }
  }
}
