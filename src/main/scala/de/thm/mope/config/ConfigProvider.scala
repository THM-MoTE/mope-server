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

import com.typesafe.config.{Config, ConfigFactory}
import java.nio.file.{Files, Path}

class ConfigProvider(
  cli:CliConf,
  userConfig:Path) {

  val config = cli.asConfig
    .withFallback(ConfigProvider.createConfigFile(userConfig))
    .withFallback(ConfigFactory.parseResources("fallback.conf"))
    .resolve()
}

object ConfigProvider {
  def createConfigFile(file:Path):Config = {
    if(Files.notExists(file)) {
      Files.createDirectories(file.getParent)
      Files.copy(getClass.getResourceAsStream("/mope.conf"), file)
    }
    ConfigFactory.parseURL(file.toUri.toURL)
  }
}