/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import de.thm.moie.project.ProjectDescription
import de.thm.moie.server.ProjectsManagerActor._
import akka.pattern.ask
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.util.Timeout
import akka.testkit.{ TestActors, TestKit, ImplicitSender, TestProbe }
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._
import scala.language.postfixOps

class ProjectsManagerSpec()
  extends TestKit(ActorSystem("specSystem")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val timeout = 5 seconds
  implicit val time = Timeout(timeout)

  val projectPath = System.getProperty("user.home") + "/Downloads"

  private def projectActor: ActorRef =
    system.actorOf(Props[ProjectsManagerActor])

  private def stubDescription =
    ProjectDescription(projectPath, "target", Nil)

  private def projectActorWithStubDescription = {
      val act = projectActor
      val descr = stubDescription
      act ! descr
      (act, descr)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A ProjectsManager" must {
    "return a project id, when sending a ProjectDescription" in {
      val (actor, descr) = projectActorWithStubDescription
      expectMsg(ProjectId(0))
    }

    "return same project id, when sending same ProjectDescription twice" in {
      val (actor, descr) = projectActorWithStubDescription
      expectMsg(ProjectId(0))

      actor ! descr
      expectMsg(ProjectId(0))
    }

    "return a Some(ActorRef) when sending a valid ProjectId" in {
      val (actor, descr) = projectActorWithStubDescription
      expectMsg(ProjectId(0))
      actor ! ProjectId(0)
      expectMsgClass(timeout, classOf[Some[ActorRef]])
    }

    "return a None when sending a unknown ProjectId" in {
      val (actor, descr) = projectActorWithStubDescription
      expectMsg(ProjectId(0))
      actor ! ProjectId(1)
      expectMsgAnyOf(timeout, None)
    }

    "return same ref when sending same ProjectId" in {
      val (actor, descr) = projectActorWithStubDescription
      val projId = expectMsg(ProjectId(0))
      actor ! projId
      val ref = expectMsgClass(timeout, classOf[Some[ActorRef]])
      actor ! projId
      val ref2 = expectMsgAnyOf(timeout, ref)
    }

    "kill ProjectManager when sending Disconnect" in {
      val (actor, descr) = projectActorWithStubDescription
      //wait until processing of first msg is done
      expectMsgClass(timeout, classOf[ProjectId])
      actor ! ProjectId(0)
      val probe = TestProbe()
      val target = expectMsgClass(timeout, classOf[Some[ActorRef]]).get
      probe watch target
      actor ! Disconnect(0)
      probe.expectTerminated(target)
    }
  }
}
