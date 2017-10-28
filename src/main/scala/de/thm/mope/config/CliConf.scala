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
import org.rogach.scallop._

import scala.collection.JavaConverters._

/** Configuration for CLI-arguments parser. */
class CliConf(args:Seq[String]) extends ScallopConf(args) {
  val protocol = opt[String]("protocol")
  val interface = opt[String]("interface")
  val port = opt[Int]("port")
  val omc = opt[String]("omc")
  verify()

  private def configKey[A](opt:ScallopOption[A], k:String):Option[(String,String)] = {
    opt.map(v => k -> v.toString).toOption
  }

  def asConfig:Config = {
    ConfigFactory.parseMap(Map(
      Seq(configKey(port, "protocol.port"),
        configKey(interface, "protocol.interface"),
      configKey(protocol, "protocol"),
      configKey(omc, "compilerExecutable")).flatten:_*
    ).asJava)
  }
}
