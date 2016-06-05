/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.pattern.pipe
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import de.thm.moie.Global
import de.thm.moie.project.ProjectDescription
import de.thm.moie.utils.actors.UnhandledReceiver
import de.thm.moie.server.ProjectRegister._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class ProjectsManagerActor
  extends Actor
  with UnhandledReceiver
  with LogMessages {

  import ProjectsManagerActor._
  import context.dispatcher


  private val register = new ProjectRegister()

  private def withIdExists[T](id:ID)(f: (ProjectDescription, ActorRef) => T):Option[T] =
    register.get(id) map {
      case ProjectEntry(descr, actor) => f(descr, actor)
    }

  private def newManager(description:ProjectDescription, id:ID): ActorRef = {
    val executableString = Global.getCompilerExecutable
    val compilerClazz = Global.getCompilerClass
    val constructor = compilerClazz.getDeclaredConstructor(classOf[List[String]], classOf[String], classOf[String])
    val compiler = constructor.newInstance(description.compilerFlags, executableString, description.outputDirectory)
    context.actorOf(Props(new ProjectManagerActor(description, compiler)), name = s"proj-manager-$id")
  }

  override def handleMsg: Receive = {
    case description:ProjectDescription =>
      Future {
        val id = register.add(description)(newManager)
        ProjectId(id)
      } pipeTo sender
    case ProjectId(id) =>
      Future(withIdExists(id) { (_, actor) => actor }) pipeTo sender
    case Disconnect(id) => Future {
      register.remove(id).map {
        case ProjectEntry(_, actor) =>
          actor ! PoisonPill
          RemainingClients(register.projectCount)
      }
    } pipeTo sender
  }

  override def postStop(): Unit = log.info("stopping")
}

object ProjectsManagerActor {
  sealed trait ProjectsManagerMsg
  case class ProjectId(id:Int) extends ProjectsManagerMsg
  case class Disconnect(id:Int) extends ProjectsManagerMsg
  case class RemainingClients(count:Int) extends ProjectsManagerMsg
}
