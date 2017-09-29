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

  val lengthRegex = """Content-Length:\s+(\d+)""".r
  val headerRegex = """\s*([\w-]+):\s+([\d\w-\/]+)\s*""".r
  implicit val log = Logging(system, getClass)

  val rpcParser = Flow[String].fold("")((acc,elem) => acc+elem)
    .map(_.replaceAll("""(\r\n?)""", "\n")) //replace windows linebreaks
    .filterNot(_.trim.isEmpty)
    .log("parsing")
    .map { s => s.parseJson.convertTo[RequestMessage] }

  def connectTo[I:JsonFormat,O:JsonFormat](userHandlers:RpcMethod[I,O]):Flow[ByteString,ByteString,NotUsed] = {
    val handlers = StreamUtils.broadcastAll(userHandlers.toFlows)

    Flow[ByteString]
      .via(new ProtocolHandler())
      .log("raw-json")
      .map { s => s.parseJson.convertTo[RequestMessage] }
      .log("parsed")
      .flatMapConcat { msg =>
        Source.single(msg)
          .via(handlers)
          .map { params =>
            val body = ResponseMessage(msg.id,params).toJson.toString
            s"""Content-Length: ${body.length}\r
             |\r
             |$body""".stripMargin
          }
      }
      .log("out")
      .map(ByteString(_))
  }
}
