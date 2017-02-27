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

import org.slf4j.{LoggerFactory}
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Level

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import de.thm.mope.Global._

import scala.concurrent.duration._
import scala.language.postfixOps

trait ServerSetup {

  val serverName = "mope-server"
  val applicationMode = ApplicationMode.parseString(config.getString("app.mode"))

  implicit val serverLogSource:LogSource[ServerSetup] = new LogSource[ServerSetup] {
    override def genString(setup:ServerSetup): String =
      setup.serverName
  }

  val akkaConfig = ConfigFactory.parseFile(
    new java.io.File(configFileURL.toURI))

  /** Overwrite as lazy val! */
  implicit def actorSystem:ActorSystem
  /** Overwrite as lazy val! */
  implicit def execContext = actorSystem.dispatcher
  def blockingDispatcher = actorSystem.dispatchers.lookup("akka.dispatchers.blocking-io")
  /** Overwrite as lazy val! */
  implicit def materializer:ActorMaterializer

  implicit val defaultTimeout = Timeout(config.getInt("defaultAskTimeout") seconds)

  val serverlog = Logging(actorSystem, this)
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  serverlog.info("{} - Version {}", build.ProjectInfo.name, build.ProjectInfo.version)
}
