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

import akka.actor.{Actor, Props}
import de.thm.mope.ActorSpec
import de.thm.mope.project.ProjectDescription

class ProjectRegisterSpec
  extends ActorSpec {
import ProjectRegister._

  private val projectPath = System.getProperty("java.io.tmpdir") + "/Downloads"
  private def stubDescription =
    ProjectDescription(projectPath, "target", None)

  private def dummyActor = new Actor {
    override def receive:Receive = { case _ => }
  }

  private def newDummy = system.actorOf(Props(dummyActor))

  "ProjectRegister" must {
    "add ProjectEntrys" in {
      val register = new ProjectRegister()
      val entry = ProjectEntry(stubDescription, system.actorOf(Props[ProjectsManagerActor]))
      val id = register.add(entry)
      register.projectCount shouldEqual 1
      register.get(id) shouldEqual Some(entry)

      val entry2 = ProjectEntry(
        ProjectDescription(projectPath, "bin", None),
        system.actorOf(Props[ProjectsManagerActor]))
      val id2 = register.add(entry2)
      register.projectCount shouldEqual 2
      register.get(id2) shouldEqual Some(entry2)
    }

    "add ProjectDescriptions" in {
      val register = new ProjectRegister()
      val id = register.add(stubDescription)( (_,_) => newDummy)

      val opt = register.get(id)
      opt shouldBe a [Some[_]]
      opt.get shouldBe a [ProjectEntry]
      register.clientCount shouldEqual 1
    }

    "increase clientCount when adding ProjectDescriptions multiple times" in {
      val register = new ProjectRegister()
      val id = register.add(stubDescription)( (_,_) => newDummy)
      val maxCnt = 6
      for(_ <- (1 until maxCnt)) {
        val newId = register.add(stubDescription)( (_,_) => newDummy)
        newId shouldEqual id
      }
        val entry = register.get(id).get
      entry.clientCnt shouldEqual maxCnt
      register.clientCount shouldEqual maxCnt
    }

    "decrease clientCount when removing by id multiple times" in {
      val register = new ProjectRegister()
      val id = register.add(stubDescription)( (_,_) => newDummy)
      val maxCnt = 6
      for(_ <- (1 until maxCnt)) {
        val newId = register.add(stubDescription)( (_,_) => newDummy)
        newId shouldEqual id
      }

      val clients = register.clientCount
      register.remove(id) shouldBe a [Some[_]]
      register.remove(id) shouldBe a [Some[_]]
      register.remove(id) shouldBe a [Some[_]]
      val entry = register.get(id).get
      entry.clientCnt shouldEqual (clients-3)
      register.clientCount shouldEqual (clients-3)
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

    "remove entries by valid ids" in {
      val register = new ProjectRegister()
      val entry = ProjectEntry(stubDescription, system.actorOf(Props[ProjectsManagerActor]))
      val id = register.add(entry)
      val entryOpt = register.remove(id)
      register.projectCount shouldEqual 0
      register.getProjects.size shouldEqual 0
      register.getDescriptionToId.size shouldEqual 0
      entryOpt shouldEqual Some(entry.copy(clientCnt=0))
    }

    "not remove entries by invalid ids" in {
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
