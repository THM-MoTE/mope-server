package de.thm.moie.doc

import java.nio.file.Files

import akka.testkit.TestActorRef
import de.thm.moie.ActorSpec
import de.thm.moie.compiler.OMCompiler
import de.thm.moie.doc.DocumentationProvider.{GetClassComment, GetDocumentation}

import scala.concurrent.duration._
import scala.language.postfixOps

class DocumentationProviderSpec extends ActorSpec {
  val path = Files.createTempDirectory("moie")
  val compiler = new OMCompiler("omc", path.resolve("target"))
  val testRef = TestActorRef[DocumentationProvider](new DocumentationProvider(compiler))

  override def afterAll: Unit = {
    de.thm.moie.removeDirectoryTree(path)
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

    "return classComment from stdlib" in {
      val exp = ClassComment("Modelica.Electrical", "Library of electrical models (analog, digital, machines, multi-phase)")
      testRef ! GetClassComment("Modelica.Electrical")
      expectMsg(Some(exp))
    }

    "return no classComment for non-existent classes" in {
      testRef ! GetClassComment("Modelica.nico")
      expectMsg(None)
    }
  }
}
