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


package de.thm.mope.server

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import de.thm.mope.config.{Constants, ServerConfig}
import de.thm.mope.utils.{
  MopeExitCodes,
  BindingWrapper
}
import de.thm.mope.Factory
import scala.concurrent.Future

class Server(bindingProvider:Factory[Future[BindingWrapper]],
             serverConfig: ServerConfig)(
              implicit
              actorSystem: ActorSystem,
              mat: ActorMaterializer)
  extends ValidateConfig {

  import actorSystem.dispatcher

  val serverlog = Logging(actorSystem, classOf[Server])
  serverlog.info("{} - Version: {} - {}", build.ProjectInfo.name, build.ProjectInfo.version, serverConfig.applicationMode)

  val errors = validateConfig(serverConfig.config)
  if (errors.nonEmpty) {
    val errorString = errors.map(x => s" - $x").mkString("\n")
    serverlog.error(
      s"""Your configuration (${Constants.configFile}) contains the following errors:
         |$errorString""".stripMargin)
    MopeExitCodes.waitAndExit(MopeExitCodes.CONFIG_ERROR)
  }


  def start(): Unit = {
    val bindingFuture = bindingProvider()

    bindingFuture onComplete {
      case scala.util.Success(_) =>
        serverlog.info("Server running at {}:{}", serverConfig.interface, serverConfig.port)
      case scala.util.Failure(ex) =>
        serverlog.error("Failed to start server at {}:{} - {}", serverConfig.interface, serverConfig.port, ex.getMessage)
        actorSystem.terminate()
    }

    serverlog.info("Press Ctrl+D to interrupt")
    while (System.in.read() != -1) {} //wait for Ctrl+D (end-of-transmission) ; EOT == -1 for JVM
    bindingFuture.
      flatMap(_.unbind()).
      onComplete(_ => actorSystem.terminate())
  }
}
