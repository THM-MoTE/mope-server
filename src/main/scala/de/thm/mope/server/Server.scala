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

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.event.Logging
import de.thm.mope.Global
import de.thm.mope.utils.MopeExitCodes
import de.thm.mope.Global.ApplicationMode
import de.thm.mope.MopeModule

import scala.concurrent.{Future, blocking}
import scala.io.StdIn

class Server()
    extends MopeModule
    with ValidateConfig {

  override implicit lazy val actorSystem = ActorSystem("moie-system", config)
  override implicit lazy val mat = ActorMaterializer()
  import actorSystem.dispatcher
  val serverlog = Logging(actorSystem, classOf[Server])
  serverlog.info("{} - Version {}", build.ProjectInfo.name, build.ProjectInfo.version)

  val errors = validateConfig(Global.config)
  if(!Global.configDidExist) {
    serverlog.error(s"""Your configuration (${Global.configFileURL}) got newly created!
                        |Please adjust the settings before continuing.""".stripMargin)
    MopeExitCodes.waitAndExit(MopeExitCodes.UNMODIFIED_CONFIG)
  } else if(errors.nonEmpty) {
    val errorString = errors.map(x => s" - $x").mkString("\n")
    serverlog.error(s"""Your configuration (${Global.configFileURL}) contains the following errors:
                        |$errorString""".stripMargin)
    MopeExitCodes.waitAndExit(MopeExitCodes.CONFIG_ERROR)
  }


  def start():Unit = {
    val bindingFuture =
      Http().bindAndHandle(router.routes, interface, port)

    bindingFuture onComplete {
      case scala.util.Success(_) =>
        serverlog.info("Server running at {}:{}", interface, port)
      case scala.util.Failure(ex) =>
        serverlog.error("Failed to start server at {}:{} - {}", interface, port, ex.getMessage)
        actorSystem.terminate()
    }

    if(applicationMode == ApplicationMode.Development) {
      Future {
        blocking {
          serverlog.info("Press Ctrl+D to interrupt")
          while (System.in.read() != -1) {} //wait for Ctrl+D (end-of-transmission) ; EOT == -1 for JVM
          bindingFuture.
            flatMap(_.unbind()).
            onComplete(_ => actorSystem.terminate())
        }
      }
    }
  }
}
