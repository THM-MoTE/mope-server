package de.thm.moie.server

import java.nio.file.Files
import akka.testkit.TestActorRef
import de.thm.moie.project.CompletionResponse.CompletionType
import de.thm.moie.project.{CompletionRequest, CompletionResponse}
import de.thm.moie.compiler.{OMCompiler, FilePosition}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await

class CodeCompletionActorSpec extends ActorSpec {
  val path = Files.createTempDirectory("moie")
  val compiler = new OMCompiler(Nil, "omc", path.resolve("target"))
  val testRef = TestActorRef[CodeCompletionActor](new CodeCompletionActor(compiler))
  val completionActor = testRef.underlyingActor

  def simpleRequest(word:String) = CompletionRequest("unknown", FilePosition(0,0), word)

  "`findClosestMatch`" should {
    "return closest match" in {
      val words = completionActor.keywords ++ completionActor.types
      val word = "an"
      val res = Await.result(completionActor.findClosestMatch(word, words), 5 seconds)
      res.toSet shouldBe Set("annotation", "and")

      val word2 = "annotation"
      Await.result(completionActor.findClosestMatch(word2, words), 5 seconds) shouldBe Set("annotation")

      val word3 = "Bool"
      Await.result(completionActor.findClosestMatch(word3, words), 5 seconds) shouldBe Set("Boolean")

      val erg = Set(
        "each",
        "else",
        "elseif",
        "elsewhen",
        "encapsulated",
        "end",
        "enumeration",
        "equation",
        "expandable",
        "extends",
        "external")
      val word4 = "e"
        Await.result(completionActor.findClosestMatch(word4, words), 5 seconds) shouldBe erg
    }
  }

  "CodeCompletionActor" should {
    "return a keyword completion" in {
      testRef ! simpleRequest("func")
      expectMsg(5 seconds, Set(CompletionResponse(CompletionType.Keyword, "function", None)))

      testRef ! simpleRequest("im")
      expectMsg(5 seconds, Set(CompletionResponse(CompletionType.Keyword, "import", None),
                                CompletionResponse(CompletionType.Keyword, "impure", None)))

      val erg = Set(
        "each",
        "else",
        "elseif",
        "elsewhen",
        "encapsulated",
        "end",
        "enumeration",
        "equation",
        "expandable",
        "extends",
        "external").map(CompletionResponse(CompletionType.Keyword, _, None))
      testRef ! simpleRequest("e")
      expectMsg(5 seconds, erg)
    }
    "return a type completion" in {
      testRef ! simpleRequest("B")
      expectMsg(5 seconds, Set(CompletionResponse(CompletionType.Type, "Boolean", None)))

      testRef ! simpleRequest("In")
      expectMsg(5 seconds, Set(CompletionResponse(CompletionType.Type, "Integer", None)))
    }

    "return package-names" in {
      testRef ! simpleRequest("Modelica.Electrical.")
      val names = Set(
      "Modelica.Electrical.Analog",
      "Modelica.Electrical.Digital",
      "Modelica.Electrical.Machines",
      "Modelica.Electrical.MultiPhase",
      "Modelica.Electrical.QuasiStationary",
      "Modelica.Electrical.Spice3")

      expectMsg(10 seconds, names.map(CompletionResponse(CompletionType.Type, _ , None)))

      val names2 = Set(
        "Modelica.UsersGuide",
        "Modelica.Blocks",
        "Modelica.ComplexBlocks",
        "Modelica.StateGraph",
        "Modelica.Electrical",
        "Modelica.Magnetic",
        "Modelica.Mechanics",
        "Modelica.Fluid",
        "Modelica.Media",
        "Modelica.Thermal",
        "Modelica.Math",
        "Modelica.ComplexMath",
        "Modelica.Utilities",
        "Modelica.Constants",
        "Modelica.Icons",
        "Modelica.SIunits")

      testRef ! simpleRequest("Modelica.")
      expectMsg(10 seconds, names2.map(CompletionResponse(CompletionType.Type, _ , None)))
    }
  }
}
