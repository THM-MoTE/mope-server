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
    .map { msg =>
      val payload = msg.toJson.prettyPrint
      ByteString(s"""Content-Type: text/utf-8\r
                   |Content-Length: ${payload.length}\r\n
                   |""".stripMargin) ++ (payload+"\r\n").getBytes("UTF-8")
    }
  val pipeline = Flow[ByteString]
    .via(Framing.delimiter(ByteString("\n"), 8024, true))
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
  }
}
