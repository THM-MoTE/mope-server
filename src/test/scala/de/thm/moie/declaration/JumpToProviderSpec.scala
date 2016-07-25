package de.thm.moie.declaration

import java.nio.file.Files

import akka.testkit.TestActorRef
import de.thm.moie.ActorSpec
import de.thm.moie.compiler.OMCompiler
import de.thm.moie.position.FilePath

import scala.concurrent.duration._
import scala.language.postfixOps

class JumpToProviderSpec extends ActorSpec {
  val path = Files.createTempDirectory("moie")
  val compiler = new OMCompiler("omc", path.resolve("target"))
  val testRef = TestActorRef[JumpToProvider](new JumpToProvider(compiler))
  val completionActor = testRef.underlyingActor

  override def afterAll: Unit = {
    de.thm.moie.removeDirectoryTree(path)
  }

  "JumpToProvider" should {
    "return the file to a class" in {
      testRef ! DeclarationRequest("Modelica.Electrical")
      expectMsg(10 seconds, Some(FilePath("/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/Electrical/package.mo")))

      testRef ! DeclarationRequest("Modelica.Electrical.Analog")
      expectMsg(10 seconds, Some(FilePath("/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/Electrical/Analog/package.mo")))
    }

    "return no file to a unknown class" in {
      testRef ! DeclarationRequest("nico")
      expectMsg(10 seconds, None)

      testRef ! DeclarationRequest("Modelica.none")
      expectMsg(10 seconds, None)
    }
  }
}
