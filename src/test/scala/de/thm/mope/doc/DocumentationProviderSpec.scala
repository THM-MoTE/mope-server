/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
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

package de.thm.mope.doc

import java.nio.file.Files

import akka.testkit.TestActorRef
import de.thm.mope.TestHelpers
import de.thm.mope.ActorSpec
import de.thm.mope.compiler.OMCompiler
import de.thm.mope.doc.DocumentationProvider.{GetClassComment, GetDocumentation}

import scala.concurrent.duration._
import scala.language.postfixOps

class DocumentationProviderSpec extends ActorSpec {
  val path = Files.createTempDirectory("moie")
  val compiler = new OMCompiler("omc", path.resolve("target"))
  val testRef = TestActorRef[DocumentationProvider](new DocumentationProvider(compiler))

  override def afterAll: Unit = {
    TestHelpers.removeDirectoryTree(path)
  }

  "DocumentationProvider" should {
    "return documentation from stdlib" in {
      val exp = """<html>
      |<p>
      |This library contains electrical components to build up analog and digital circuits,
      |as well as machines to model electrical motors and generators,
      |especially three phase induction machines such as an asynchronous motor.
      |</p>
      |
      |</html>""".stripMargin

      testRef ! GetDocumentation("Modelica.Electrical")
      val info = expectMsgType[Option[DocInfo]](5 seconds)
      info.get.info shouldBe exp

      val exp2 = """<html>
          |<p>The linear resistor connects the branch voltage <i>v</i> with the branch current <i>i</i> by <i>i*R = v</i>. The Resistance <i>R</i> is allowed to be positive, zero, or negative.</p>
          |</html>""".stripMargin
      testRef ! GetDocumentation("Modelica.Electrical.Analog.Basic.Resistor")
      val info2 = expectMsgType[Option[DocInfo]](5 seconds)
      info2.get.info shouldBe exp2
    }

    "return no documentation for non-existent classes" in {
      testRef ! GetDocumentation("nico")
      expectMsg(5 seconds, None)
    }
  }
}
