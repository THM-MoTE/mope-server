/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import org.scalatest._
import akka.testkit.TestActorRef
import de.thm.moie.project.ProjectDescription
import de.thm.moie._
import de.thm.moie.server.ProjectManagerActor._
import de.thm.moie.compiler._
import java.nio.file._
import java.nio.charset.StandardCharsets

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ProjectManagerActorSpec
  extends ActorSpec {

  val path = Files.createTempDirectory("moie")
  val projectPath = path.resolve("mo-project")

  Files.createDirectory(projectPath)

  override def afterAll = {
    super.afterAll()
    removeDirectoryTree(path)
  }
  private def stubDescription =
    ProjectDescription(projectPath.toAbsolutePath().toString(), "target", Nil, None)

  private def dummyError(x:Path) = CompilerError("Error", x.toAbsolutePath().toString(), FilePosition(0,0), FilePosition(0,0), "")

  val testRef = TestActorRef[ProjectManagerActor](new ProjectManagerActor(stubDescription, new OMCompiler(List(), "omc", projectPath.resolve(stubDescription.outputDirectory)), false))
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

    val allMoFiles = createdMoFiles
    val deletedFiles = allMoFiles.take(2)
    val remainingFiles = allMoFiles.drop(2)

    "contain only existent files" in {
      val fileList = manager.getProjectFiles
      val files = Await.result(fileList, 10 seconds)
      files.toSet shouldEqual allMoFiles.toSet
    }

    "contain existent files which are sorted" in {
      val fileList = manager.getProjectFiles
      val files = Await.result(fileList, 10 seconds)
      files shouldEqual allMoFiles.sorted
    }

    "remove files if they got deleted" in {
      deletedFiles.foreach(Files.delete(_))
      Thread.sleep(5000)
      val fileList = manager.getProjectFiles
      val files = Await.result(fileList, 10 seconds)
      files.toSet shouldEqual remainingFiles.toSet
    }

    "contain sorted files even if some file got deleted" in {
      val fileList = manager.getProjectFiles
      val files = Await.result(fileList, 10 seconds)
      files shouldEqual remainingFiles.sorted
    }

    "return 1 compile error for invalid file" in {
      val invalidFile = createInvalidFile(projectPath)

      //test errors
      require(Files.exists(invalidFile), "file has to be created!")
//      Thread.sleep(20000)
      val files = manager.getProjectFiles
      println("files in test: "+files)
      testRef ! CompileProject(invalidFile)
      val xs = expectMsgType[List[CompilerError]](10 seconds)
      xs.size shouldBe 1
      xs.head shouldBe invalidFileError(invalidFile)
      Files.deleteIfExists(invalidFile)
    }

    "return 0 compile errors for valid file" in {
      val validFile = createValidFile(projectPath)
        //test errors
      testRef ! CompileProject(validFile)
      val xs = expectMsgType[List[CompilerError]](10 seconds)
      xs shouldBe empty
    }

    "return compile errors for invalid scripts" in {
      val scriptFile = createInvalidScript(projectPath)
        //test errors
      testRef ! CompileScript(scriptFile)
      val xs = expectMsgType[List[CompilerError]](10 seconds)
      xs.size shouldBe 1
      xs.head shouldBe invalidScriptError(scriptFile)
    }

    "fail if opened file doesn't exist" in {
      testRef ! CompileProject(projectPath.resolve("unknown"))
      val failure = expectMsgType[akka.actor.Status.Failure](10 seconds)
      failure.cause.isInstanceOf[NotFoundException] shouldBe true

      testRef ! CompileScript(projectPath.resolve("unknownscript"))
      val failure2 = expectMsgType[akka.actor.Status.Failure](10 seconds)
      failure2.cause.isInstanceOf[NotFoundException] shouldBe true
    }

    "return 0 compile errors for valid scripts" in {
      val validScriptFile = createValidScript(projectPath)
      //test errors
      testRef ! CompileScript(validScriptFile)
      val xs = expectMsgType[List[CompilerError]](10 seconds)
      xs shouldBe empty
    }
  }
}
