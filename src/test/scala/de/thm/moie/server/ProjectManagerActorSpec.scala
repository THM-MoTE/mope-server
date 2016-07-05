/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import org.scalatest._
import akka.testkit.{ TestActorRef }
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
  val scriptFile = projectPath.resolve("script.mos")

  Files.createDirectory(projectPath)
  Files.createFile(testFile)
  Files.createFile(scriptFile)

  override def afterAll = {
    super.afterAll()
    removeDirectoryTree(path)
  }
  private def stubDescription =
    ProjectDescription(projectPath.toAbsolutePath().toString(), "target", Nil, None)

  private def dummyError(x:Path) = CompilerError("Error", x.toAbsolutePath().toString(), FilePosition(0,0), FilePosition(0,0), "")

  val testRef = TestActorRef[ProjectManagerActor](new ProjectManagerActor(stubDescription, new OMCompiler(List(), "omc", projectPath.resolve(stubDescription.outputDirectory))))
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
    val createdMoFiles = List(
      projectPath.resolve("test2.mo"),
      projectPath.resolve("nico.mo"),
      projectPath.resolve("model.mo"),
      projectPath.resolve("model2.mo")
    )
    createdMoFiles.foreach(Files.createFile(_))

    val allMoFiles = testFile :: createdMoFiles
    val deletedFiles = allMoFiles.take(2)
    val remainingFiles = allMoFiles.drop(2)

    "return 1 compile error for invalid file" in {
        //write file content
      val contentWithErrors = """
      |model myModel
      |   Rel number
      |end myModel;
      """.stripMargin

      val bw = Files.newBufferedWriter(testFile, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)
      bw.write(contentWithErrors)
      bw.write("\n")
      bw.close()

      Thread.sleep(1000) //give OS time to throw a CREATE event

        //test errors
      testRef ! CompileProject
      val xs = expectMsgType[List[CompilerError]](10 seconds)
      xs.size should be (1)
    }

    "return 0 compile errors for valid file" in {
        //write file content
      val contentWithErrors = """
      |model myModel
      |   Real number;
      |end myModel;
      """.stripMargin

      val bw = Files.newBufferedWriter(testFile, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)
      bw.write(contentWithErrors)
      bw.write("\n")
      bw.close()

      Thread.sleep(1000) //wait till buffers are written

        //test errors
      testRef ! CompileProject
      val xs = expectMsgType[List[CompilerError]](10 seconds)
      xs.size should be (0)
    }

    "return compile errors for invalid scripts" in {
      val scriptContent = """
lodFile("bouncing_ball.mo");
""".stripMargin

            val bw = Files.newBufferedWriter(scriptFile, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)
      bw.write(scriptContent)
      bw.write("\n")
      bw.close()

      Thread.sleep(1000) //wait till buffers are written

        //test errors
      testRef ! CompileScript(scriptFile)
      val xs = expectMsgType[List[CompilerError]](10 seconds)
      xs.size should be > 1
    }

    "return 0 compile errors for valid scripts" in {
      val scriptContent = """
loadFile("bouncing_ball.mo");
""".stripMargin

      val bw = Files.newBufferedWriter(scriptFile, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)
      bw.write(scriptContent)
      bw.write("\n")
      bw.close()

      Thread.sleep(1000) //wait till buffers are written

      //test errors
      testRef ! CompileScript(scriptFile)
      val xs = expectMsgType[List[CompilerError]](10 seconds)
      xs.size should be (0)
    }

    "contain only existent files" in {
      Thread.sleep(8000) //wait a bit; until all OS events are processed
      val fileList = manager.getProjectFiles
      fileList.toSet shouldEqual allMoFiles.toSet
    }

    "contain existent files which are sorted" in {
      // Thread.sleep(10000) //wait a bit; until all OS events are processed
      val fileList = manager.getProjectFiles
      fileList shouldEqual allMoFiles.sorted
    }

    "remove files if they got deleted" in {
      deletedFiles.foreach(Files.delete(_))
      Thread.sleep(8000)
      val fileList = manager.getProjectFiles
      fileList.toSet shouldEqual remainingFiles.toSet
    }

    "contain sorted files even if some file got deleted" in {
      val fileList = manager.getProjectFiles
      fileList shouldEqual remainingFiles.sorted
    }
  }
}
