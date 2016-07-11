/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import org.scalatest._
import java.nio.file._

import de.thm.moie._
class OMCompilerTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  val path = Files.createTempDirectory("moie")
  val compiler = new OMCompiler(Nil, "omc", path.resolve("target"))

  override def afterAll(): Unit = {
    super.afterAll()
    compiler.stop()
    removeDirectoryTree(path)
  }

  "OMCompiler" should "return no errors for valid modelica files" in {
    val files = List(createValidFile(path))
    compiler.compile(files, files.head) shouldBe empty
  }

  it should "return 1 error for invalid modelica files" in {
    val files = List(createInvalidFile(path))
    compiler.compile(files, files.head) shouldBe List(invalidFileError(files.head))
  }

  it should "return no errors for valid modelica script files" in {
    val file = createValidScript(path)
    compiler.compileScript(file) shouldBe empty
  }

  it should "return 1 error for invalid modelica script files" in {
    val file = createInvalidScript(path)
    compiler.compileScript(file) shouldBe List(invalidScriptError(file))
  }
}
