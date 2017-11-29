/**
  * Copyright (C) 2016,2017 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */


package de.thm.mope.suggestion

import java.nio.file.Files

import akka.testkit.TestActorRef
import de.thm.mope.{ActorSpec, TestHelpers}
import de.thm.mope.compiler.OMCompiler
import de.thm.mope.position.FilePosition
import de.thm.mope.suggestion.Suggestion.Kind

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class SuggestionProviderSpec extends ActorSpec {
  val path = Files.createTempDirectory("moie")
  val compiler = new OMCompiler("omc", path.resolve("target"))
  val testRef = TestActorRef[SuggestionProvider](new SuggestionProvider(compiler))
  val completionActor = testRef.underlyingActor

  override def afterAll: Unit = {
    TestHelpers.removeDirectoryTree(path)
  }
  def simpleRequest(word:String) = CompletionRequest("unknown", FilePosition(0,0), word)

  "CodeCompletionActor" should {
    "return a keyword completion" in {
      testRef ! simpleRequest("func")
      expectMsg(5 seconds, Set(Suggestion(Kind.Keyword, "function", None, None, None)))

      testRef ! simpleRequest("im")
      expectMsg(5 seconds, Set(Suggestion(Kind.Keyword, "import", None, None, None),
                                Suggestion(Kind.Keyword, "impure", None, None, None)))

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
        "external").map(Suggestion(Kind.Keyword, _, None, None, None))
      testRef ! simpleRequest("e")
      expectMsg(5 seconds, erg)
    }
    "return a type completion" in {
      testRef ! simpleRequest("B")
      expectMsg(5 seconds, Set(Suggestion(Kind.Type, "Boolean", None, None, None)))

      testRef ! simpleRequest("In")
      expectMsg(5 seconds, Set(Suggestion(Kind.Type, "Integer", None, None, None)))
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
        case (name, classComment) => Suggestion(Kind.Package, name, None, Some(classComment), None)
      }

      val msg = expectMsgType[Set[_]](10 seconds)
      //expectMsg(10 seconds, exp)
      println(s"msg:\n$msg\nexp:\n$exp")

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
        case (name, classComment) => Suggestion(Kind.Package, name , None, Some(classComment), None)
      }

      testRef ! simpleRequest("Modelica.")
      expectMsg(10 seconds, exp2)
    }

    "return <global-scope> package-names" in {
      val exp = Set(
        "Modelica" -> "Modelica Standard Library - Version 3.2.1 (Build 4)",
        "ModelicaServices" -> "ModelicaServices (OpenModelica implementation) - Models and functions used in the Modelica Standard Library requiring a tool specific implementation").
        map {
          case (name, classComment) => Suggestion(Kind.Package, name , None, Some(classComment), None)
        }

      testRef ! simpleRequest("Mod")
      expectMsg(10 seconds, exp)
      testRef ! simpleRequest("M")
      expectMsg(10 seconds, exp)
    }

    "return empty-set for empty string" in {
      testRef ! simpleRequest("")
      expectMsg(10 seconds, Set.empty[Suggestion])
    }

    "return names for started classes" in {
      val exp = Set(Suggestion(Kind.Package, "Modelica.Electrical",
        None,
        Some("Library of electrical models (analog, digital, machines, multi-phase)"),
        None))
      testRef ! simpleRequest("Modelica.Elec")
      expectMsg(10 seconds, exp)

      val exp2 = Set(
        "Modelica.Magnetic" -> "Library of magnetic models",
        "Modelica.Math" -> "Library of mathematical functions (e.g., sin, cos) and of functions operating on vectors and matrices").
        map {
        case (name, classComment) => Suggestion(Kind.Package, name , None, Some(classComment), None)
      }
      testRef ! simpleRequest("Modelica.Ma")
      expectMsg(10 seconds, exp2)
    }
  }
}
