/**
  * Copyright (C) 2016,2017 Nicola Justus <nicola.justus@mni.thm.de>
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
class CliConf(args: Seq[String]) extends ScallopConf(args) {
  version(s"${build.ProjectInfo.name} ${build.ProjectInfo.version} ${build.ProjectInfo.copyright}")

  val protocol = opt[String]("protocol", descr="the protocol to use, either `http` or `lsp`")
  val interface = opt[String]("interface", descr="the interface to use, defaults to `localhost`")
  val port = opt[Int]("port", descr="the port of the protocol to use")
  val compiler = opt[String]("compiler", descr="the executable of the compiler to use")
  verify()

  private def configKey[A](opt: ScallopOption[A], k: String): Option[(String, String)] = {
    opt.map(v => k -> v.toString).toOption
  }

  def asConfig: Config = {
    ConfigFactory.parseMap(Map(
      Seq(configKey(port, "protocol.port"),
        configKey(interface, "protocol.interface"),
        configKey(protocol, "protocol.type"),
        configKey(compiler, "compilerExecutable")).flatten: _*
    ).asJava)
  }
}
