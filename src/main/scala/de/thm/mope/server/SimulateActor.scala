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

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import de.thm.mope.compiler.{CompilerError, ModelicaCompiler}
import de.thm.mope.models.SimulationResult
import de.thm.mope.config.ProjectConfig
import de.thm.mope.utils.actors.{MopeActor, UnhandledReceiver}
import scala.concurrent.Future
import scala.language.postfixOps


private class SimulateWorker(
  compiler: ModelicaCompiler,
  projConfig: ProjectConfig)
    extends MopeActor {

  implicit val blockingIO = projConfig.server.blockingDispatcher

  override def receive:Receive = {
    case SimulateActor.SimulateModel(modelName,options) =>
      val f = Future(compiler.simulate(modelName, options))
        .flatMap(Future.fromTry(_))
        .map(res => SimulationResult(modelName, res))
      context.become(running(f))
  }

  private def running(f:Future[SimulationResult]): Receive = {
    case SimulateActor.SimulationId(_) if f.isCompleted => f.map(Some(_)) pipeTo sender
    case _ => sender ! None
  }
}

class SimulateActor(
  compiler: ModelicaCompiler,
  projConfig: ProjectConfig)
    extends MopeActor {
  private val workerProps = Props(classOf[SimulateWorker], compiler, projConfig)

  override def receive:Receive = {
    case msg:SimulateActor.SimulateModel =>
      log.info("creating worker for {}", msg)
      val id = java.util.UUID.randomUUID().toString
      val child = context.actorOf(workerProps, name=id)
      child ! msg
      sender ! SimulateActor.SimulationId(id)
    case SimulateActor.SimulationId(id) =>
      val selection = context.actorSelection(id)
      log.debug("searching for {} is {}", id:Any, selection:Any)
      selection forward SimulateActor.SimulationId(id)
  }
}

object SimulateActor {
  case class SimulationId(id:String)
  case class SimulateModel(modelName:String, options:Map[String,String])
}
