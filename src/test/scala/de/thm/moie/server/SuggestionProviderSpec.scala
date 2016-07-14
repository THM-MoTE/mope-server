package de.thm.moie.server

import java.nio.file.Files
import akka.testkit.TestActorRef
import de.thm.moie.project.CompletionResponse.CompletionType
import de.thm.moie.project.{CompletionRequest, CompletionResponse}
import de.thm.moie.compiler.{OMCompiler, FilePosition}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Await

class SuggestionProviderSpec extends ActorSpec {
  val path = Files.createTempDirectory("moie")
  val compiler = new OMCompiler(Nil, "omc", path.resolve("target"))
  val testRef = TestActorRef[SuggestionProvider](new SuggestionProvider(compiler))
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
      expectMsg(5 seconds, Set(CompletionResponse(CompletionType.Keyword, "function", None, None)))

      testRef ! simpleRequest("im")
      expectMsg(5 seconds, Set(CompletionResponse(CompletionType.Keyword, "import", None, None),
                                CompletionResponse(CompletionType.Keyword, "impure", None, None)))

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
        "external").map(CompletionResponse(CompletionType.Keyword, _, None, None))
      testRef ! simpleRequest("e")
      expectMsg(5 seconds, erg)
    }
    "return a type completion" in {
      testRef ! simpleRequest("B")
      expectMsg(5 seconds, Set(CompletionResponse(CompletionType.Type, "Boolean", None, None)))

      testRef ! simpleRequest("In")
      expectMsg(5 seconds, Set(CompletionResponse(CompletionType.Type, "Integer", None, None)))
    }

    "return package-names" in {
      testRef ! simpleRequest("Modelica.Electrical.")
      val exp = Set(
      "Modelica.Electrical.Analog" -> "Library for analog electrical models",
      "Modelica.Electrical.Digital" -> "Library for digital electrical components based on the VHDL standard with 9-valued logic and conversion to 2-,3-,4-valued logic",
      "Modelica.Electrical.Machines" -> "Library for electric machines",
      "Modelica.Electrical.MultiPhase" -> "Library for electrical components with 2, 3 or more phases",
      "Modelica.Electrical.QuasiStationary" -> "Library for quasi-stationary electrical singlephase and multiphase AC simulation",
      "Modelica.Electrical.Spice3" -> "Library for components of the Berkeley SPICE3 simulator").
      map {
        case (name, classComment) => CompletionResponse(CompletionType.Package, name, None, Some(classComment))
      }

      expectMsg(10 seconds, exp)

      val exp2 = Set(
        "Modelica.UsersGuide" -> "User's Guide",
        "Modelica.Blocks"-> "Library of basic input/output control blocks (continuous, discrete, logical, table blocks)",
        "Modelica.ComplexBlocks" -> "Library of basic input/output control blocks with Complex signals",
        "Modelica.StateGraph" -> "Library of hierarchical state machine components to model discrete event and reactive systems",
        "Modelica.Electrical" -> "Library of electrical models (analog, digital, machines, multi-phase)",
        "Modelica.Magnetic" -> "Library of magnetic models",
        "Modelica.Mechanics" -> "Library of 1-dim. and 3-dim. mechanical components (multi-body, rotational, translational)",
        "Modelica.Fluid" -> "Library of 1-dim. thermo-fluid flow models using the Modelica.Media media description",
        "Modelica.Media" -> "Library of media property models",
        "Modelica.Thermal" -> "Library of thermal system components to model heat transfer and simple thermo-fluid pipe flow",
        "Modelica.Math" -> "Library of mathematical functions (e.g., sin, cos) and of functions operating on vectors and matrices",
        "Modelica.ComplexMath" -> "Library of complex mathematical functions (e.g., sin, cos) and of functions operating on complex vectors and matrices",
        "Modelica.Utilities" -> "Library of utility functions dedicated to scripting (operating on files, streams, strings, system)",
        "Modelica.Constants" -> "Library of mathematical constants and constants of nature (e.g., pi, eps, R, sigma)",
        "Modelica.Icons" -> "Library of icons",
        "Modelica.SIunits" -> "Library of type and unit definitions based on SI units according to ISO 31-1992").
      map {
        case (name, classComment) => CompletionResponse(CompletionType.Package, name , None, Some(classComment))
      }

      testRef ! simpleRequest("Modelica.")
      expectMsg(10 seconds, exp2)
    }

    "return <global-scope> package-names" in {
      val exp = Set(
        "Modelica" -> "Modelica Standard Library - Version 3.2.1 (Build 4)",
        "ModelicaServices" -> "ModelicaServices (OpenModelica implementation) - Models and functions used in the Modelica Standard Library requiring a tool specific implementation").
        map {
          case (name, classComment) => CompletionResponse(CompletionType.Package, name , None, Some(classComment))
        }

      testRef ! simpleRequest("Mod")
      expectMsg(10 seconds, exp)
      testRef ! simpleRequest("M")
      expectMsg(10 seconds, exp)
    }

    "return empty-set for empty string" in {
      testRef ! simpleRequest("")
      expectMsg(10 seconds, Set.empty[CompletionResponse])
    }

    "return names for started classes" in {
      val exp = Set(CompletionResponse(CompletionType.Package, "Modelica.Electrical",
        None,
        Some("Library of electrical models (analog, digital, machines, multi-phase)")))
      testRef ! simpleRequest("Modelica.Elec")
      expectMsg(10 seconds, exp)

      val exp2 = Set(
        "Modelica.Magnetic" -> "Library of magnetic models",
        "Modelica.Math" -> "Library of mathematical functions (e.g., sin, cos) and of functions operating on vectors and matrices").
        map {
        case (name, classComment) => CompletionResponse(CompletionType.Package, name , None, Some(classComment))
      }
      testRef ! simpleRequest("Modelica.Ma")
      expectMsg(10 seconds, exp2)
    }
  }
}
