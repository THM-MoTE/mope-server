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

import java.nio.file.{Files, Path, Paths}

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import de.thm.mope.config.Constants
import de.thm.mope.utils.actors.UnhandledReceiver
import de.thm.recent.JsProtocol._
import de.thm.recent._

import scala.concurrent.Future

class RecentFilesActor(recentFilesPath: Path)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import RecentFilesActor._
  import context._


  var recent = Recent.fromList(List[Path]())

  override def preStart(): Unit = {
    val recent =
      if (Files.exists(recentFilesPath)) Recent.fromInputStream[Path](Files.newInputStream(recentFilesPath))
      else Recent.fromList(List[Path]())

    log.info("Initialized with file {}", recentFilesPath)
    log.debug("Recent files are {}", recent.recentElements)
    self ! Initialized(recent)
  }

  override def receive: Receive = {
    case Initialized(newRecent) =>
      recent = newRecent
      become(initialized)
  }

  private def initialized: Receive = {
    case GetRecentFiles => Future(recent.recentElementsByPriority) pipeTo sender
    case AddStr(fileStr) =>
      val path = Paths.get(fileStr)
      log.debug("Increment priority of {}", path)
      recent = recent.incrementPriority(path)
  }

  override def postStop(): Unit = {
    log.info("Writing {} recent files into {}", recent.recentElements.size, recentFilesPath)
    Files.write(recentFilesPath, recent.toJson.getBytes(Constants.encoding))
  }
}

object RecentFilesActor {

  case object GetRecentFiles

  case class Initialized(r: Recent[Path])

  case class AddStr(p: String)

}
