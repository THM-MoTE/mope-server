package de.thm.moie.server

import org.scalatest._
import akka.util.Timeout
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.{ TestActors, TestActorRef, TestKit, ImplicitSender, TestProbe }
import de.thm.moie.project.ProjectDescription
import de.thm.moie._
import de.thm.moie.server.ProjectManagerActor._
import de.thm.moie.compiler._
import java.nio.file._
import java.nio.charset.StandardCharsets
import scala.concurrent.duration._
import scala.language.postfixOps

class ProjectManagerActorSpec
  extends ActorSpec {

  val path = Files.createTempDirectory("moie")
  val projectPath = path.resolve("mo-project")

  val testFile = projectPath.resolve("test.mo")

  override def beforeAll = {
    Files.createDirectory(projectPath)
  }

  override def afterAll = {
    super.afterAll()
    removeDirectoryTree(path)
  }
  private def stubDescription =
    ProjectDescription(projectPath.toAbsolutePath().toString(), "target", Nil)

  private def dummyError(x:Path) = CompilerError("Error", x.toAbsolutePath().toString(), FilePosition(0,0), FilePosition(0,0), "")

  val testRef = TestActorRef[ProjectManagerActor](new ProjectManagerActor(stubDescription, new OMCompiler(List(), "omc", stubDescription.outputDirectory)))
  val manager = testRef.underlyingActor
  "ProjectManager's `errorInProjectFile`" should {
    "filter pathes that aren't childpathes from `mo-project`" in {
      val xs = List(
        Paths.get("/home/nico/Downloads"),
        Paths.get("/home/nico/hans"),
        projectPath.getParent(),
        projectPath.resolve("test/util.mo"),
        projectPath.resolve("test/util3.mo"),
        projectPath.resolve("project.mo")
      ).map(dummyError)

      xs.filter(manager.errorInProjectFile) shouldEqual List(projectPath.resolve("test/util.mo"),
        projectPath.resolve("test/util3.mo"),
        projectPath.resolve("project.mo")).map(dummyError)
    }
  }

  "ProjectManager" must {
    "return compile errors for existing files" in {
        //write file content
      val contentWithErrors = """
      |model myModel
      |   Rl number;
      |end myModel;
      """.stripMargin

      Files.createFile(testFile)
      val bw = Files.newBufferedWriter(testFile, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)
      bw.write(contentWithErrors)
      bw.close()

        //test errors
      testRef ! CompileProject
      val xs = expectMsgType[List[CompilerError]](5 seconds)
      xs.size should be (1)
    }
  }
}
