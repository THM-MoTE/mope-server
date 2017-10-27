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

import com.typesafe.config.Config
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Path, Paths}
import java.util.concurrent.Executors
import akka.util.Timeout
import akka.dispatch.Dispatcher

case class ServerConfig(
  config:Config,
  encoding:Charset = StandardCharsets.UTF_8,
  configDir:Path = Paths.get(System.getProperty("user.home"), ".mope"),
  recentFiles:Path = configDir.resolve("recent-files.json"))(
  implicit
    timeout:Timeout,
    blockingDispatcher:Dispatcher) {
  val applicationMode = ApplicationMode.parseString(config.getString("app.mode"))
  val executor = Executors.unconfigurableExecutorService(blockingDispatcher)
}
