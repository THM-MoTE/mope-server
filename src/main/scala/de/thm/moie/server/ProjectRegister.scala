/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.ActorRef
import scala.collection.mutable
import de.thm.moie.project.ProjectDescription

class ProjectRegister() {
  import ProjectRegister._

  private var idCounter:Int = 0
  private val projects = mutable.HashMap[ID, ProjectEntry]()
  private val descriptionToId = mutable.HashMap[ProjectDescription, ID]()

  private def withNewId[A](f: ID => A):A = {
    val res = f(idCounter)
    idCounter += 1
    res
  }

  def add(e:ProjectEntry):ID = {
    descriptionToId.get(e.description) match {
      case Some(id) => id
      case None => withNewId { newID =>
        projects += newID -> e
        descriptionToId += e.description -> newID
        newID
      }
    }
  }

  def add(descr:ProjectDescription)(f: (ProjectDescription, ID) => ActorRef):ID = {
    descriptionToId.get(descr) match {
      case Some(id) => id
      case None =>
        withNewId { newID =>
          val actor = f(descr, newID)
          projects += newID -> ProjectEntry(descr, actor)
          descriptionToId += descr -> newID
          newID
        }
    }
  }

  def get(id:ID) = projects.get(id)

  def remove(id:ID): Option[ProjectEntry] = {
    val entryOpt = projects remove id
    entryOpt.
      map { x =>
        descriptionToId remove x.description
        x
      }
  }

  def projectCount:Int = projects.size

  def getProjects:Map[ID, ProjectEntry] = projects.toMap
  def getDescriptionToId:Map[ProjectDescription, ID] = descriptionToId.toMap
}

object ProjectRegister {
  type ID = Int
  case class ProjectEntry(description:ProjectDescription, actor:ActorRef)
}
