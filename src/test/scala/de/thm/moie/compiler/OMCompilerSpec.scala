/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import org.scalatest._
import java.nio.file._

import de.thm.moie._
class OMCompilerSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  val path = Files.createTempDirectory("moie")
  val compiler = new OMCompiler(Nil, "omc", path.resolve("target"))

  override def afterAll(): Unit = {
    super.afterAll()
    compiler.stop()
    removeDirectoryTree(path)
  }

  val files = List(createValidFile(path))

  "OMCompiler" should "return no errors for valid modelica files" in {
    compiler.compile(files, files.head) shouldBe empty
  }

  it should "return 1 error for invalid modelica files" in {
    val files = List(createInvalidFile(path))
    compiler.compile(files, files.head) shouldBe List(invalidFileError(files.head))
  }

  it should "return 1 type-error for invalid modelica files" in {
    val files = List(createSemanticInvalidFile(path))
    compiler.compile(files, files.head) shouldBe List(semanticInvalidFileError(files.head))
  }

  it should "return no errors for valid modelica script files" in {
    val file = createValidScript(path)
    compiler.compileScript(file) shouldBe empty
  }

  it should "return 1 error for invalid modelica script files" in {
    val file = createInvalidScript(path)
    compiler.compileScript(file) shouldBe List(invalidScriptError(file))
  }

  it should "return # of equations" in {
    val erg = compiler.checkModel(files, files.head)
    println(erg)
    println("starts \n: "+erg.startsWith("\n"))
    println("ends \n: "+erg.endsWith("\n"))
    compiler.checkModel(files, files.head) shouldBe (
      """Check of test completed successfully.
      |Class test has 2 equation(s) and 1 variable(s).
      |2 of these are trivial equation(s).""".stripMargin)
  }
}
