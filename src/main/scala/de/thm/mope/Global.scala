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

package de.thm.mope

import java.net.URL
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths}

import com.typesafe.config.{Config, ConfigFactory}

import scala.io.Source

object Global extends FallbackConfig {

  object ApplicationMode extends Enumeration {
    val Development, Production = Value

    def parseString(str:String): Value = str.toLowerCase match {
      case "dev" | "development" => Development
      case "prod" | "production" => Production
      case _ => throw new IllegalArgumentException(s"Can't decide which mode $str represents")
    }
  }

  private val configDirPath = Paths.get(System.getProperty("user.home"), ".config", "mope")

  /** Check if path exist; if not create it */
  def withCheckConfigDirectory[A](fn: Path => A): A = {
    if(Files.notExists(configDirPath))
      Files.createDirectory(configDirPath)
    fn(configDirPath)
  }

  /** Copies the file from classpath to filePath if filePath doesn't exist */
  def copyIfNotExist(filePath:Path, filename:String): Boolean = {
    if(Files.notExists(filePath)) {
      val is = getClass.getResourceAsStream("/"+filename)
      Files.copy(is, filePath)
      false
    } else true
  }

  /** Returns the absolute config-url from relativePath */
  private def getConfigFile(relativePath:String):(URL, Boolean) =
    withCheckConfigDirectory { configPath =>
      val filePath = configPath.resolve(relativePath)
      val flag = copyIfNotExist(filePath, relativePath)
      (filePath.toUri.toURL, flag)
    }


  def readValuesFromResource(path:URL)(filter: Filter[String]): List[String] = {
    Source.fromURL(path, encoding.displayName).getLines.flatMap {
      case s:String if filter(s) => List(s)
      case a:Any => Nil
    }.toList
  }

  lazy val encoding = Charset.forName("UTF-8")
  lazy val (configFileURL, configDidExist) = getConfigFile("mope.conf")
  lazy val recentFilesPath = configDirPath.resolve("recent-files.json")
  lazy val config: Config = ConfigFactory.parseURL(configFileURL).withFallback(fallbackConfig)
  lazy val usLocale = "en_US.UTF-8"
}
