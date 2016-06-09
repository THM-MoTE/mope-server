/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import org.scalatest._
import akka.util.Timeout
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.{ TestActors, TestKit, ImplicitSender, TestProbe }
import de.thm.moie.project.ProjectDescription

class ProjectRegisterSpec
  extends ActorSpec {
import ProjectRegister._

  private val projectPath = System.getProperty("user.home") + "/Downloads"
  private def stubDescription =
    ProjectDescription(projectPath, "target", Nil)

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  "ProjectRegister" must {
    "add ProjectEntrys" in {
      val register = new ProjectRegister()
      val entry = ProjectEntry(stubDescription, system.actorOf(Props[ProjectsManagerActor]))
      val id = register.add(entry)
      register.projectCount shouldEqual 1
      register.get(id) shouldEqual Some(entry)

      val entry2 = ProjectEntry(
        ProjectDescription(projectPath, "bin", Nil),
        system.actorOf(Props[ProjectsManagerActor]))
      val id2 = register.add(entry2)
      register.projectCount shouldEqual 2
      register.get(id2) shouldEqual Some(entry2)
    }

    "not add same ProjectEntry twice" in {
      val register = new ProjectRegister()
      val entry = ProjectEntry(stubDescription, system.actorOf(Props[ProjectsManagerActor]))
      val id = register.add(entry)
      val id2 = register.add(entry)
      id shouldEqual id2
      register.projectCount shouldEqual 1
    }

    "get same ProjectEntry when giving same id" in {
      val register = new ProjectRegister()
      val entry = ProjectEntry(stubDescription, system.actorOf(Props[ProjectsManagerActor]))
      val id = register.add(entry)
      val entry1 = register.get(id)
      val entry2 = register.get(id)
      entry1 shouldEqual entry2
    }

    "remove entrys by valid ids" in {
      val register = new ProjectRegister()
      val entry = ProjectEntry(stubDescription, system.actorOf(Props[ProjectsManagerActor]))
      val id = register.add(entry)
      val entryOpt = register.remove(id)
      register.projectCount shouldEqual 0
      register.getProjects.size shouldEqual 0
      register.getDescriptionToId.size shouldEqual 0
      entryOpt shouldEqual Some(entry)
    }

    "not remove entrys by invalid ids" in {
      val register = new ProjectRegister()
      val entry = ProjectEntry(stubDescription, system.actorOf(Props[ProjectsManagerActor]))
      val id = register.add(entry)
      val invalidId = id+1
      val entryOpt = register.remove(invalidId)
      register.projectCount shouldEqual 1
      register.getDescriptionToId.size shouldEqual 1
      register.getDescriptionToId.size shouldEqual 1
      entryOpt shouldEqual None
    }
  }
}
