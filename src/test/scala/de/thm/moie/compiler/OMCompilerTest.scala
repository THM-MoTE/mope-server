/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import org.scalatest._
import java.nio.file._

import de.thm.moie._
class OMCompilerTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  val path = Files.createTempDirectory("moie")
  val projectPath = path.resolve("mo-compiler-project")
  val compiler = new OMCompiler(Nil, "omc", projectPath.resolve("target"))

  val file =
    projectPath.resolve("simple.mo") ->
      """
      model simple
        Real cnt = 1;
      equation
        der(cnt) = cnt*(-1);
      end simple;
      """.stripMargin
  val (filepath, content) = file
  val files = List(filepath)

  override def beforeAll() = {
    Files.createDirectories(projectPath)
    val bw = Files.newBufferedWriter(filepath)
    bw.write(content)
    bw.close()
  }

  override def afterAll() = {
    removeDirectoryTree(path)
    compiler.stop()
  }

  "Compiler" should "return no errors for valid modelica files" in {
    compiler.compile(files) shouldEqual Nil
  }
  it should "return 1 error for file with 1 error" in {
    val newContent =
        """
        model simple
          Ral cnt = 1
        equation
          der(cnt) = cnt*(-1);
        end simple;
        """.stripMargin
    val bw = Files.newBufferedWriter(filepath, StandardOpenOption.TRUNCATE_EXISTING)
    bw.write(newContent)
    bw.close()
    val errors = compiler.compile(files)
    errors.size shouldEqual 1
    errors.head.message shouldEqual "Missing token: SEMICOLON"

    val newContent2 =
      """
      model simpe
        Real cnt = 1;
      equation
        der(cnt) = cnt*(-1);
      end simple;
      """.stripMargin
    val bw2 = Files.newBufferedWriter(filepath, StandardOpenOption.TRUNCATE_EXISTING)
    bw2.write(newContent2)
    bw2.close()
    val errors2 = compiler.compile(files)
    errors2.size shouldEqual 1
    errors2.head.message shouldEqual "Parse error: The identifier at start and end are different"
  }

  it should "return an error for invalid modelica script files" in {
    val scriptContent =
      """
      |lodFile("bam");
      """.stripMargin
      val bw = Files.newBufferedWriter(filepath, StandardOpenOption.TRUNCATE_EXISTING)
      bw.write(scriptContent)
      bw.close()

      val errors = compiler.compileScript(filepath)
      errors.size shouldEqual 1
      errors.head.message shouldBe
        ("Klasse lodFile konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden.")
  }
}
