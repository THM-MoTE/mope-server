/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file._

import akka.testkit.TestActorRef
import de.thm.moie._
import de.thm.moie.compiler._
import de.thm.moie.position.FilePosition
import de.thm.moie.project.ProjectDescription
import de.thm.moie.server.ProjectManagerActor._
import de.thm.moie.declaration.DeclarationRequest
import de.thm.moie.position._
import de.thm.moie.doc._
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
    ProjectDescription(projectPath.toAbsolutePath().toString(), "target", None)

  private def dummyError(x:Path) = CompilerError("Error", x.toAbsolutePath().toString(), FilePosition(0,0), FilePosition(0,0), "")

  val testRef = TestActorRef[ProjectManagerActor](new ProjectManagerActor(stubDescription, new OMCompiler("omc", projectPath.resolve(stubDescription.outputDirectory)), false))
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

    "not filter modelica scripts" in {
      val xs = List(
        Paths.get("/test.mo"),
        Paths.get("/test2.mos"),
        projectPath.resolve("test.mo")).
        map(dummyError)

      xs.filter(manager.errorInProjectFile) shouldEqual
        (List(Paths.get("/test2.mos"), projectPath.resolve("test.mo")).
          map(dummyError))
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
      val files = manager.getProjectFiles
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

    "return the source of a valid symbol" in {
      testRef ! DeclarationRequest("Modelica.Electrical")
      val fp = expectMsgType[Option[FileWithLine]](5 seconds)
      fp.get.path.isEmpty() should be (false)
      fp.get.path.contains("Modelica") shouldBe true
    }

    "return no source for unknown symbol" in {
      testRef ! DeclarationRequest("nico")
      val fp = expectMsgType[Option[FileWithLine]](5 seconds)
      fp shouldBe None
    }

    "return a `DocInfo` for a symbol" in {
      testRef ! DocumentationProvider.GetDocumentation("Modelica.Electrical")
      val docOpt = expectMsgType[Option[DocInfo]](5 seconds)
      docOpt shouldBe defined
      docOpt.get.info.isEmpty() shouldBe (false)
    }

    "return no `DocInfo` for a unknown symbol" in {
      testRef ! DocumentationProvider.GetDocumentation("nico")
      val docOpt = expectMsgType[Option[DocInfo]](5 seconds)
      docOpt shouldBe None
    }

    "fail if opened file doesn't exist" in {
      testRef ! CompileProject(projectPath.resolve("unknown"))
      val failure = expectMsgType[akka.actor.Status.Failure](10 seconds)
      failure.cause.isInstanceOf[NotFoundException] shouldBe true

      testRef ! CompileScript(projectPath.resolve("unknownscript"))
      val failure2 = expectMsgType[akka.actor.Status.Failure](10 seconds)
      failure2.cause.isInstanceOf[NotFoundException] shouldBe true
    }

    "return compile errors for invalid scripts" in {
      val scriptFile = createInvalidScript(projectPath)
        //test errors
      testRef ! CompileScript(scriptFile)
      val xs = expectMsgType[List[CompilerError]](10 seconds)
      xs.size shouldBe 1
      xs.head shouldBe invalidScriptError(scriptFile)
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
