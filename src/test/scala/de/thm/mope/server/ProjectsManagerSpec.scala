/**
  * Copyright (C) 2016,2017 Nicola Justus <nicola.justus@mni.thm.de>
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

import java.nio.file.Files

import akka.actor.{ActorRef, Props}
import akka.testkit.TestProbe
import akka.util.Timeout
import de.thm.mope.TestHelpers
import de.thm.mope.ActorSpec
import de.thm.mope.project.ProjectDescription
import de.thm.mope.server.ProjectsManagerActor._

import scala.concurrent.duration._
import scala.language.postfixOps

class ProjectsManagerSpec()
  extends ActorSpec {

  val timeout = 5 seconds
  implicit val time = Timeout(timeout)

  val projPath = Files.createTempDirectory("moie")
  val projectPath = projPath.toAbsolutePath().toString()

  private def projectActor: ActorRef =
    system.actorOf(Props[ProjectsManagerActor])

  private def stubDescription =
    ProjectDescription(projectPath, "target", None)

  private def projectActorWithStubDescription = {
      val act = projectActor
      val descr = stubDescription
      act ! descr
      (act, descr)
  }

  override def afterAll(): Unit = {
    TestHelpers.removeDirectoryTree(projPath)
  }

  "A ProjectsManager" must {
    "return a project id, when sending a ProjectDescription" in {
      val (actor, descr) = projectActorWithStubDescription
      expectMsg(Right(ProjectId(0)))
    }

    "return same project id, when sending same ProjectDescription twice" in {
      val (actor, descr) = projectActorWithStubDescription
      expectMsg(Right(ProjectId(0)))

      actor ! descr
      expectMsg(Right(ProjectId(0)))
    }

    "return a Some(ActorRef) when sending a valid ProjectId" in {
      val (actor, descr) = projectActorWithStubDescription
      expectMsg(Right(ProjectId(0)))
      actor ! ProjectId(0)
      expectMsgClass(timeout, classOf[Some[ActorRef]])
    }

    "return a None when sending a unknown ProjectId" in {
      val (actor, descr) = projectActorWithStubDescription
      expectMsg(Right(ProjectId(0)))
      actor ! ProjectId(1)
      expectMsgAnyOf(timeout, None)
    }

    "return same ref when sending same ProjectId" in {
      val (actor, descr) = projectActorWithStubDescription
      val projId = expectMsg(Right(ProjectId(0))).right.get
      actor ! projId
      val ref = expectMsgClass(timeout, classOf[Some[ActorRef]])
      actor ! projId
      val ref2 = expectMsgAnyOf(timeout, ref)
    }

    "kill ProjectManager when sending Disconnect" in {
      val (actor, descr) = projectActorWithStubDescription
      //wait until processing of first msg is done
      expectMsgClass(timeout, classOf[Right[_,_]])
      actor ! ProjectId(0)
      val probe = TestProbe()
      val target = expectMsgClass(timeout, classOf[Some[ActorRef]]).get
      probe watch target
      actor ! Disconnect(0)
      expectMsg(timeout, Some(RemainingClients(0)))
      probe.expectTerminated(target)
    }

    "not kill ProjectManager if there are multiple clients for same project" in {
      val (actor, descr) = projectActorWithStubDescription
      //wait until processing of first msg is done
      expectMsgClass(timeout, classOf[Right[_,_]])
      actor ! descr
      expectMsgClass(timeout, classOf[Right[_,_]])
      actor ! Disconnect(0)
      expectMsg(timeout, Some(RemainingClients(1)))
      actor ! Disconnect(0)
      expectMsg(timeout, Some(RemainingClients(0)))
    }
  }
}
