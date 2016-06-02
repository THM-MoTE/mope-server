package de.thm.moie.server

import akka.pattern.pipe
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import de.thm.moie.Global
import de.thm.moie.project.ProjectDescription
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class ProjectsManagerActor
  extends Actor
  with UnhandledReceiver
  with LogMessages {

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
            val executableString = Global.getCompilerExecutable
            val compilerClazz = Global.getCompilerClass
            val constructor = compilerClazz.getDeclaredConstructor(classOf[List[String]], classOf[String], classOf[String])
            val compiler = constructor.newInstance(description.compilerFlags, executableString, description.ouputDirectory)

            val manager = context.actorOf(Props(new ProjectManagerActor(description, compiler)), name = s"proj-manager-$size")
            projects += ((description, manager))
            ProjectId(size)
        }
      } pipeTo sender
    case ProjectId(id) => Future(projects(id)._2) pipeTo sender
    case Disconnect(id) => Future {
      val (_, actor) = projects(id)
      projects.remove(id)
      actor ! PoisonPill
    }
  }

  override def postStop(): Unit = log.info("stopping")
}

object ProjectsManagerActor {
  sealed trait ProjectsManagerMsg
  case class ProjectId(id:Int) extends ProjectsManagerMsg
  case class Disconnect(id:Int) extends ProjectsManagerMsg
}