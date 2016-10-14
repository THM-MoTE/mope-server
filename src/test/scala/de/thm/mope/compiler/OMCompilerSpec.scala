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

package de.thm.mope.compiler

import java.nio.file._

import de.thm.mope._
import org.scalatest._
class OMCompilerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  val projPath = Files.createTempDirectory("moie")
  val compiler = new OMCompiler("omc", projPath.resolve("target"))

  override def afterAll(): Unit = {
    super.afterAll()
    compiler.stop()
    removeDirectoryTree(projPath)
  }

  val files = List(createValidFile(projPath))

  "OMCompiler" should "return no errors for valid modelica files" in {
    compiler.compile(files, files.head) shouldBe empty
  }

  it should "return 1 error for invalid modelica files" in {
    val files = List(createInvalidFile(projPath))
    compiler.compile(files, files.head) shouldBe List(invalidFileError(files.head))
  }

  it should "return 1 type-error for invalid modelica files" in {
    val files = List(createSemanticInvalidFile(projPath))
    compiler.compile(files, files.head) shouldBe List(semanticInvalidFileError(files.head))
  }

  it should "return no errors for valid modelica script files" in {
    val file = createValidScript(projPath)
    compiler.compileScript(file) shouldBe empty
  }

  it should "return 1 error for invalid modelica script files" in {
    val file = createInvalidScript(projPath)
    compiler.compileScript(file) shouldBe List(invalidScriptError(file))
  }

  it should "return # of equations" in {
    val erg = compiler.checkModel(files, files.head)
    compiler.checkModel(files, files.head) shouldBe (
      """Check of test completed successfully.
      |Class test has 2 equation(s) and 1 variable(s).
      |2 of these are trivial equation(s).""".stripMargin)
  }
}
