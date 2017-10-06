package de.thm.mope.lsp

import akka.actor.{ActorRef, Props}
import akka.testkit.TestProbe
import akka.testkit.TestActorRef
import akka.util.Timeout
import de.thm.mope.TestHelpers
import de.thm.mope.ActorSpec
import messages.{Position, Range}

import java.nio.file._
import scala.concurrent.duration._
import scala.language.postfixOps

class BufferContentActorSpec
    extends ActorSpec {

  "BufferContentActor" should {
    val actor = TestActorRef[BufferContentActor]
    val content = """this is a super
                    |awesome content.
                    |what are you doing tonight?
                    |hope i see you tonight..""".stripMargin

    "hold the content of a file/buffer" in {
      val contentMsg = BufferContentActor.BufferContent(Paths.get("test.file"), content)
      actor ! contentMsg
      actor.underlyingActor.currentContent should be (contentMsg)
    }

    "return the text of the buffer in a given range" in {
      actor ! BufferContentActor.GetContentRange(Some(Range(Position(1,0), Position(2,4))))
      expectMsg("""awesome content.
                   |what""".stripMargin)
    }
    "return the whole content if range omitted" in {
      actor ! BufferContentActor.GetContentRange()
      expectMsg(content)
    }
  }
}
