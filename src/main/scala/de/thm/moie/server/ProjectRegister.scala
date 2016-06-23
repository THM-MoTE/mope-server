/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.ActorRef
import de.thm.moie.project.ProjectDescription

import scala.collection.mutable

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
      case Some(id) =>
        val entry = projects(id)
        projects += id -> entry.copy(clientCnt=entry.clientCnt+1)
        id
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
    val entryOpt = projects get id
    entryOpt.
      map { x =>
        if(x.clientCnt == 1) { //now removing last client
          projects remove id
          descriptionToId remove x.description
          x.copy(clientCnt=0)
        } else {
          projects += id -> x.copy(clientCnt=x.clientCnt-1)
          x
        }
      }
  }

  def projectCount:Int = projects.size
  def clientCount:Int = projects.map {
    case (_, ProjectEntry(_,_,cnt)) => cnt
  }.sum

  def getProjects:Map[ID, ProjectEntry] = projects.toMap
  def getDescriptionToId:Map[ProjectDescription, ID] = descriptionToId.toMap
}

object ProjectRegister {
  type ID = Int
  case class ProjectEntry(description:ProjectDescription, actor:ActorRef, clientCnt:Int=1)
}
