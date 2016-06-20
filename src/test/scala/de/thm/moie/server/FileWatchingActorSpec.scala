/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.Props
import akka.testkit.{ TestActors, TestKit }
import de.thm.moie.project.ProjectDescription
import de.thm.moie.server.FileWatchingActor._
import de.thm.moie.utils.ResourceUtils
import java.nio.file._
import de.thm.moie._

class FileWatchingActorSpec() extends ActorSpec {
  import timeouts._
  val path = Files.createTempDirectory("moie")
  val projectPath = path.resolve("mo-project")
  val emptyPath = path.resolve("empty")
  val project = ProjectDescription(
    projectPath.toAbsolutePath().toString(),
    "target", Nil)

  val files = List(
    projectPath.resolve("test.mo"),
    projectPath.resolve("test.txt"),
    projectPath.resolve("model.mo"),
    projectPath.resolve("model.test"),
    projectPath.resolve("util/model2.mo"),
    projectPath.resolve("util/resistor.mo"),
    projectPath.resolve("common/transistor.mo"),
    projectPath.resolve("common/test/algorithm.mo"),
    projectPath.resolve("common/test/algorithm.java"),
    emptyPath.resolve("test/test.txt"),
    emptyPath.resolve("test.txt"),
    emptyPath.resolve("rechnung.java"),
    emptyPath.resolve("common/uebung.java")
  )

  val dirs = List(
    projectPath.resolve("util"),
    projectPath.resolve("common/test"),
    emptyPath.resolve("test"),
    emptyPath.resolve("common")
  )

  override def beforeAll = {
    //create tmp Files
    dirs.foreach(Files.createDirectories(_))
    files.foreach(Files.createFile(_))
  }

  override def afterAll = {
    super.afterAll()
    removeDirectoryTree(path)
  }

  "A FileWatcher" should {
    "return only *.mo files" in {
      val watchingActor = system.actorOf(Props(new FileWatchingActor(Paths.get(project.path), project.outputDirectory)))
      val expectedSet =
        files.zip(files.map(ResourceUtils.getFilename)).
        filter { case (_, name) => name.endsWith(".mo") }.map(_._1).toSet
      watchingActor ! GetFiles
      val xs = expectMsgType(defaultTime)(scala.reflect.classTag[List[Path]])
      xs.toSet shouldEqual expectedSet
    }

    "return empty list if there aren't *.mo files" in {
      val watchingActor = system.actorOf(Props(new FileWatchingActor(emptyPath, project.outputDirectory)))
      val exp = List.empty[Path]
      watchingActor ! GetFiles
      expectMsg(exp)
    }
  }
}
