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

package de.thm.mope.declaration

import java.nio.file.Files

import akka.testkit.TestActorRef
import de.thm.mope.ActorSpec
import de.thm.mope.compiler.OMCompiler
import de.thm.mope.position.FileWithLine

import scala.concurrent.duration._
import scala.language.postfixOps

class JumpToProviderSpec extends ActorSpec {
  val path = Files.createTempDirectory("moie")
  val compiler = new OMCompiler("omc", path.resolve("target"))
  val testRef = TestActorRef[JumpToProvider](new JumpToProvider(compiler))
  val completionActor = testRef.underlyingActor

  override def afterAll: Unit = {
    de.thm.mope.removeDirectoryTree(path)
  }

  "JumpToProvider" should {
    "return the file to a class" in {
      testRef ! DeclarationRequest("Modelica.Electrical")
      expectMsg(10 seconds, Some(FileWithLine("/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/Electrical/package.mo", 1)))

      testRef ! DeclarationRequest("Modelica.Electrical.Analog")
      expectMsg(10 seconds, Some(FileWithLine("/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/Electrical/Analog/package.mo", 1)))
    }

    "return no file to a unknown class" in {
      testRef ! DeclarationRequest("nico")
      expectMsg(10 seconds, None)

      testRef ! DeclarationRequest("Modelica.none")
      expectMsg(10 seconds, None)
    }
  }
}
