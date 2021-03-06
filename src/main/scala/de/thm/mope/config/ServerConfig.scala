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

package de.thm.mope.config

import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Path, Paths}
import java.util.concurrent.ExecutorService

import akka.dispatch.MessageDispatcher
import akka.util.Timeout
import com.softwaremill.tagging._
import com.typesafe.config.Config
import de.thm.mope.tags.RecentFileMarker

//currently not in use (since 2019-04-18)
case class SimulationConfig(numberOfIntervals:Int)

/** Settings for the whole server (aka environment). */
case class ServerConfig(
                         config: Config,
                         executor: ExecutorService,
                         configDir: Path = Constants.configDir,
                         simulation: SimulationConfig = SimulationConfig(5),
                         recentFiles: Path @@ RecentFileMarker = Constants.recentFiles)(
                         implicit
                         val timeout: Timeout,
                         val blockingDispatcher: MessageDispatcher) {
  val applicationMode = ApplicationMode.parseString(config.getString("app.mode"))

  lazy val interface = config.getString("protocol.interface")
  lazy val port = config.getInt("protocol.port")
  lazy val compilerExecutable = config.getString("compilerExecutable")
}

object Constants {
  val encoding: Charset = StandardCharsets.UTF_8
  val usLocale = "en_US.UTF-8"
  val configDir = Paths.get(System.getProperty("user.home"), ".config", "mope")
  val configFile = configDir.resolve("mope.conf")
  val recentFiles = configDir.resolve("recent-files.json").taggedWith[RecentFileMarker]
}
