package de.thm.mope.lsp

import akka.actor.PoisonPill
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Framing, Sink, Source}
import akka.util.ByteString
import de.thm.mope.ActorSpec
import de.thm.mope.lsp.messages.RequestMessage
import de.thm.mope.server.JsonSupport
import spray.json._

import scala.concurrent.Future

class ProtocolHandlerSpec extends ActorSpec with JsonSupport {

  val inputElems = List.tabulate(5)(i => RequestMessage(2, "compile", (i+50).toJson) )

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

  val pipeline = Flow[ByteString]
    .via(new ProtocolHandler())

  "The LS-Protocol-Handler" should {
    import de.thm.mope.TestHelpers._

    "handle incoming messages" in {
      val fut = Source(inputElems)
        .via(createMsg)
        .via(pipeline)
        .runWith(Sink.seq)

      whenReady(fut) { seq =>
        listAssert(seq.map(_.parseJson.convertTo[RequestMessage]), inputElems)
      }
    }
    "handle delayed incoming requests" in {
      val fut = Source.actorRef[RequestMessage](5, OverflowStrategy.dropNew)
        .mapMaterializedValue { ref =>
          Future {
            for(elem <- inputElems) {
              Thread.sleep(200) //delay element
              ref ! elem
            }
            ref ! PoisonPill
          }
        }
      .via(createMsg)
      .via(pipeline)
      .runWith(Sink.seq)

      whenReady(fut) { seq =>
        listAssert(seq.map(_.parseJson.convertTo[RequestMessage]), inputElems)
      }
    }

    "handle one message without a last linebreak" in {
      val fut = Source.actorRef[RequestMessage](5, OverflowStrategy.dropNew)
        .mapMaterializedValue { ref =>
          Future {
              Thread.sleep(200) //delay element
              ref ! inputElems.head
          }
        }
        .via(createMsg)
        .via(pipeline)
        .runWith(Sink.head)
      whenReady(fut) { msg =>
        msg.parseJson.convertTo[RequestMessage] should be (inputElems.head)
      }
    }
  }
}
