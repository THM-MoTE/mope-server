/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.mope.server

import java.nio.file._
import java.util.concurrent.Executors

import akka.actor.{Actor, Props}
import akka.util.Timeout
import de.thm.mope.{ActorSpec, TestHelpers}
import de.thm.mope.project.{InternalProjectConfig, ProjectDescription}
import de.thm.mope.server.FileWatchingActor._
import de.thm.mope.utils.ResourceUtils

import scala.concurrent.duration._
import scala.language.postfixOps

class FileWatchingActorSpec() extends ActorSpec {
  import timeouts._
  val path = Files.createTempDirectory("moie")
  val projectPath = path.resolve("mo-project")
  val emptyPath = path.resolve("empty")
  val project = ProjectDescription(
    projectPath.toAbsolutePath().toString(),
    "target", None)

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
    TestHelpers.removeDirectoryTree(path)
  }

  val dummyActor = system.actorOf(Props(new Actor {
    override def receive:Receive = {
      case _ =>
    }
  }))

  implicit val projConfig = InternalProjectConfig(Executors.newSingleThreadExecutor(), Timeout(5 seconds))

  "A FileWatcher" should {
    "return only *.mo files" in {
      val watchingActor = system.actorOf(Props(new FileWatchingActor(dummyActor, Paths.get(project.path), project.outputDirectory)))
      val expectedSet =
        files.zip(files.map(ResourceUtils.getFilename)).
        filter { case (_, name) => name.endsWith(".mo") }.map(_._1).toSet
      watchingActor ! GetFiles
      val xs = expectMsgType(defaultTime)(scala.reflect.classTag[List[Path]])
      xs.toSet shouldEqual expectedSet
    }

    "return empty list if there aren't *.mo files" in {
      val watchingActor = system.actorOf(Props(new FileWatchingActor(dummyActor, emptyPath, project.outputDirectory)))
      val exp = List.empty[Path]
      watchingActor ! GetFiles
      expectMsg(exp)
    }
  }
}
