package de.thm.moie.server

import java.nio.file.Files

import akka.testkit.TestActorRef
import de.thm.moie.compiler.OMCompiler
import de.thm.moie.project.DocInfo
import de.thm.moie.server.DocumentationProvider.GetDocumentation
import scala.concurrent.duration._
import scala.language.postfixOps

class DocumentationProviderSpec extends ActorSpec {
  val path = Files.createTempDirectory("moie")
  val compiler = new OMCompiler(Nil, "omc", path.resolve("target"))
  val testRef = TestActorRef[DocumentationProvider](new DocumentationProvider(compiler))

  override def afterAll: Unit = {
    de.thm.moie.removeDirectoryTree(path)
  }

  "DocumentationProvider" should {
    "return documentation from stdlib" in {
      val exp = Some(DocInfo("""<html>
      |<p>
      |This library contains electrical components to build up analog and digital circuits,
      |as well as machines to model electrical motors and generators,
      |especially three phase induction machines such as an asynchronous motor.
      |</p>
      |
      |</html>""".stripMargin,"",""))

      testRef ! GetDocumentation("Modelica.Electrical")
      expectMsg(5 seconds, exp)

      val exp2 = Some(DocInfo("""<html>
          |<p>The linear resistor connects the branch voltage <i>v</i> with the branch current <i>i</i> by <i>i*R = v</i>. The Resistance <i>R</i> is allowed to be positive, zero, or negative.</p>
          |</html>""".stripMargin,
        """<html>
          |<ul>
          |<li><i> August 07, 2009   </i>
          |       by Anton Haumer<br> temperature dependency of resistance added<br>
          |       </li>
          |<li><i> March 11, 2009   </i>
          |       by Christoph Clauss<br> conditional heat port added<br>
          |       </li>
          |<li><i> 1998   </i>
          |       by Christoph Clauss<br> initially implemented<br>
          |       </li>
          |</ul>
          |</html>""".stripMargin,
        ""))
      testRef ! GetDocumentation("Modelica.Electrical.Analog.Basic.Resistor")
      val info = expectMsgType[Option[DocInfo]](5 seconds)
      info.get.info shouldBe exp2.get.info
      info.get.revisions shouldBe exp2.get.revisions
    }

    "return no documentation for non-existent classes" in {
      testRef ! GetDocumentation("nico")
      expectMsg(5 seconds, None)
    }
  }
}
