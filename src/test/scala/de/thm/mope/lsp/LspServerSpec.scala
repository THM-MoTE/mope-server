package de.thm.mope.lsp

import akka.actor.PoisonPill
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, Framing, Keep, Sink, Source }
import akka.util.ByteString
import de.thm.mope.ActorSpec
import de.thm.mope.lsp.messages._
import de.thm.mope.server.JsonSupport
import spray.json._

import scala.concurrent.Future

class LspServerSpec extends ActorSpec with JsonSupport {

  val inputElem = RequestMessage(2, "double", 50.toJson)
  val outputElem = ResponseMessage(inputElem.id, Some((50*2).toJson), None)

  val handler = RpcMethod("double", None){ i:Int => Future.successful(i*2) }

  val createMsg = Flow[RequestMessage]
    .mapConcat { msg =>
      val payload = msg.toJson.prettyPrint
      List(
        ByteString("Content-Type: text/utf-8\r\n"),
        ByteString(s"Content-Length: ${payload.length}\r\n"),
        ByteString("\r\n"),
        ByteString(payload)
      )
    }


  "The LspServer" should {
    val server = new LspServer()
    val pipe = server.connectTo(handler)

    val pipeline = Flow[RequestMessage]
      .via(createMsg)
      .via(pipe)
      .toMat(Sink.fold(""){ (acc,elem) => acc+elem.utf8String })(Keep.right)



    "return messages with \\r\\n line-terminators" in {
      whenReady(Source.single(inputElem).toMat(pipeline)(Keep.right).run()) { str =>
        str.count(_=='\n') should be (str.count(_=='\n'))
        str.split('\n') should have size (3)
      }
    }
    "return messages with a Content-Length" in {
      whenReady(Source.single(inputElem).toMat(pipeline)(Keep.right).run()) { str =>
        val expJson = outputElem.toJson.toString
        str.split("\r\n")  should contain (s"Content-Length: ${expJson.size}")
      }
    }
    "return messages with a payload" in {
      whenReady(Source.single(inputElem).toMat(pipeline)(Keep.right).run()) { str =>
        val expJson = outputElem.toJson.toString
        str.split("\r\n") should contain (expJson)
        val actualJson = str.split("\r\n").find(_==expJson).get
        actualJson.parseJson.convertTo[ResponseMessage] should be (outputElem)
      }
    }
    s"return error ${ErrorCodes.MethodNotFound} for unknown methods" in {
      val elem = RequestMessage(1, "unknown", 50.toJson)
      whenReady(Source.single(elem).toMat(pipeline)(Keep.right).run()) { str =>
        val actualJson = str.split("\r\n").last
        val msg = actualJson.parseJson.convertTo[ResponseMessage]
        msg.error.get.code should be (ErrorCodes.MethodNotFound)
        msg.error.get.message should be ("Method 'unknown' not found")
      }
    }
    s"return error ${ErrorCodes.ParseError} for falsy json objects" in {
      val elem = RequestMessage(2, "double", false.toJson)
      whenReady(Source.single(elem).toMat(pipeline)(Keep.right).run()) { str =>
        val actualJson = str.split("\r\n").last
        val msg = actualJson.parseJson.convertTo[ResponseMessage]
        msg.error.get.code should be (ErrorCodes.ParseError)
      }
    }
  }
}
