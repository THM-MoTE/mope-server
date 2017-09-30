package de.thm.mope.lsp

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream._
import akka.util.ByteString
import akka.stream.scaladsl._
import de.thm.mope.lsp.messages.{RequestMessage, ResponseMessage}
import spray.json._
import de.thm.mope.server.JsonSupport
import de.thm.mope.utils.StreamUtils

class LspServer(implicit val system:ActorSystem)
    extends JsonSupport {

  implicit val log = Logging(system, getClass)

  def connectTo[I:JsonFormat,O:JsonFormat](userHandlers:RpcMethod[I,O]):Flow[ByteString,ByteString,NotUsed] = {
    val handlers = StreamUtils.broadcastAll(userHandlers.toFlows)

    Flow[ByteString]
      .via(new ProtocolHandler())
      .map { s => s.parseJson.convertTo[RequestMessage] }
      .log("in")
      .flatMapConcat { msg =>
        Source.single(msg)
          .via(handlers)
          .log("out")
          .map { params =>
            val body = ResponseMessage(msg.id,params).toJson.toString
            s"""Content-Length: ${body.length}\r
             |\r
             |$body""".stripMargin
          }
      }
      .map(ByteString(_))
  }
}
