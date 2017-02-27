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

import akka.pattern.pipe
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import de.thm.mope.Global
import de.thm.mope.utils.actors.UnhandledReceiver
import de.thm.recent._
import JsProtocol._

import scala.concurrent.Future

class RecentFilesActor
	extends Actor
	with UnhandledReceiver
	with ActorLogging {

	import RecentFilesActor._
	import context._

	var recent = Recent.fromList(List[Path]())

	override def preStart(): Unit = {
		val recent =
			if(Files.exists(Global.recentFilesPath)) Recent.fromInputStream[Path](Files.newInputStream(Global.recentFilesPath))
			else Recent.fromList(List[Path]())

		log.info("Initialized with file {}", Global.recentFilesPath)
		log.debug("Recent files are {}", recent.recentElements)
		self ! Initialized(recent)
	}

	override def receive:Receive = {
		case Initialized(newRecent) =>
			log.debug("initialized with {}", newRecent)
			recent = newRecent
			become(initialized)
		case PoisonPill => log.info("got poisonpill in 'receive'")
	}

	private def initialized:Receive = {
		case GetRecentFiles => Future(recent.recentElementsByPriority) pipeTo sender
		case AddStr(fileStr) =>
			val path = Paths.get(fileStr)
			log.debug("increment priority of {}", path)
			recent = recent.incrementPriority(path)
	}

	override def postStop():Unit = {
		log.debug("Writing {} recent files into {}", recent.recentElements.size, Global.recentFilesPath)
		Files.write(Global.recentFilesPath, recent.toJson.getBytes(Global.encoding))
	}
}

object RecentFilesActor {
	case object GetRecentFiles
	case class Initialized(r:Recent[Path])
	case class AddStr(p:String)
}