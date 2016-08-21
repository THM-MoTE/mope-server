/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file.Paths

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import de.thm.moie.Global
import de.thm.moie.project.ProjectDescription
import de.thm.moie.server.ProjectRegister._
import de.thm.moie.utils.actors.UnhandledReceiver

/** Root actor for all registered projects. */
class ProjectsManagerActor
  extends Actor
  with UnhandledReceiver
  with ActorLogging {

  import ProjectsManagerActor._

  private val register = new ProjectRegister()

  private val indexFiles = Global.config.getBoolean("indexFiles")

  private def withIdExists[T](id:ID)(f: (ProjectDescription, ActorRef) => T):Option[T] =
    register.get(id) map {
      case ProjectEntry(descr, actor, _) => f(descr, actor)
    }

  private def newManager(description:ProjectDescription, id:ID): ActorRef = {
    val outputPath = Paths.get(description.path).resolve(description.outputDirectory)
    try {
      val compiler = Global.newCompilerInstance(outputPath)
      log.info("new manager for id:{}", id)
      context.actorOf(Props(new ProjectManagerActor(description, compiler, indexFiles)), name = s"proj-manager-$id")
    } catch {
      case ex:Exception =>
        log.error("Couldn't initialize a new ProjectManager - blow up system")
        throw ex
    }
  }

  override def receive: Receive = {
    case description:ProjectDescription =>
      val errors = ProjectDescription.validate(description)
      if(errors.isEmpty) {
        val id = register.add(description)(newManager)
        log.debug("Client registered for id:{}", id)
        sender ! Right(ProjectId(id))
      } else sender ! Left(errors)
    case ProjectId(id) =>
      sender ! withIdExists(id) { (_, actor) => actor }
    case Disconnect(id) =>
      sender ! register.remove(id).map {
        case ProjectEntry(_, actor, 0) =>
          actor ! PoisonPill
          RemainingClients(register.clientCount)
        case ProjectEntry(_,_,_) =>
          RemainingClients(register.clientCount)
      }
      log.info("Client {} disconnected; remaining clients {}", id, register.clientCount)
  }

  override def postStop(): Unit = log.info("stopping")
}

object ProjectsManagerActor {
  sealed trait ProjectsManagerMsg
  case class ProjectId(id:Int) extends ProjectsManagerMsg
  case class Disconnect(id:Int) extends ProjectsManagerMsg
  case class RemainingClients(count:Int) extends ProjectsManagerMsg
}
