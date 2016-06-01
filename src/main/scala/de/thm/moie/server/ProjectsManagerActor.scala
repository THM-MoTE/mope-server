package de.thm.moie.server

import akka.pattern.pipe
import akka.actor.{Actor, ActorRef, Props}
import de.thm.moie.project.ProjectDescription
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class ProjectsManagerActor extends Actor with UnhandledReceiver {

  import ProjectsManagerActor._
  import context.dispatcher

  private var projects = ArrayBuffer[(ProjectDescription, ActorRef)]()

  override def handleMsg: Receive = {
    case description:ProjectDescription =>
      Future {
        val existingEntry = projects.find {
          case (descr, _) => descr.path == description.path
        }

        existingEntry match {
          case Some(descr) => ProjectId(projects.indexOf(descr))
          case None =>
            val size = projects.size
            val manager = context.actorOf(Props(new ProjectManagerActor(description)), name = s"proj-manager-$size")
            projects += ((description, manager))
            ProjectId(size)
        }
      } pipeTo sender
    case ProjectId(id) => Future(projects(id)) pipeTo sender
  }
}

object ProjectsManagerActor {
  sealed trait ProjectsManagerMsg
  case class ProjectId(id:Int) extends ProjectsManagerMsg
}