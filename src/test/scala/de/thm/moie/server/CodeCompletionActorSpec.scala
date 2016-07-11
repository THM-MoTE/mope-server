package de.thm.moie.server

import akka.testkit.TestActorRef
import de.thm.moie.compiler.OMCompiler
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await

class CodeCompletionActorSpec extends ActorSpec {
  val testRef = TestActorRef[CodeCompletionActor](new CodeCompletionActor())
  val completionActor = testRef.underlyingActor

  "`findClosestMatch`" should {
    "return closest match" in {
      val word = "an"
      val res = Await.result(completionActor.findClosestMatch(word), 5 seconds)
      res.toSet shouldBe Set("annotation", "and")

      val word2 = "annotation"
      Await.result(completionActor.findClosestMatch(word2), 5 seconds) shouldBe List("annotation")

      val word3 = "Bool"
      Await.result(completionActor.findClosestMatch(word3), 5 seconds) shouldBe List("Boolean")
    }
  }
}
