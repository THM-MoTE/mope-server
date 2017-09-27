package de.thm.mope.lsp

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream._
import akka.util.ByteString
import akka.stream.scaladsl._
import spray.json._
import com.typesafe.config.ConfigFactory
import de.thm.mope.server.JsonSupport
import de.thm.mope.utils.StreamUtils

class LspServer(implicit val system:ActorSystem)
    extends JsonSupport {

  val lengthRegex = """Content-Length:\s+(\d+)""".r
  val headerRegex = """\s*([\w-]+):\s+([\d\w-\/]+)\s*""".r

  val logger = Logging(system, getClass)

  val rpcParser = Flow[String].fold("")((acc,elem) => acc+elem)
    .filterNot(_.trim.isEmpty)
    .map{s => println(s"before-json $s"); s}
    .map { s => s.parseJson.convertTo[RpcMsg] }

  def connectTo[I:JsonFormat,O:JsonFormat](userHandlers:RpcHandler[I,O]):Flow[ByteString,ByteString,NotUsed] = {
    val handlers = StreamUtils.broadcastAll(userHandlers.toFlows)
    Flow[ByteString]
      .via(Framing.delimiter(ByteString("\n"), 8024, true))
      .map(_.utf8String)
      .map{str => println("inspect: "+str) ; str}
      .splitWhen(_.matches(lengthRegex.regex))
      .filterNot(s => s.matches(headerRegex.regex))
      .map{str => println("after-split: "+str) ; str}
      .via(rpcParser)//TODO: call user handler
      .log("after-rpc")
      .flatMapConcat { msg =>
        logger.debug("got {}", msg)
        Source.single(msg)
          .via(handlers)
        .map { params =>
          ResponseMessage(msg.id,params).toJson.toString
        }.log("after-handling")
          .map { body =>
          s"""
             |Content-Length: ${body.length}
             |
             |$body
           """.stripMargin
        }
      }
      .mergeSubstreams
      .map(ByteString(_))
  }
}
