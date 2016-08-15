/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import de.thm.moie.Global
import de.thm.moie.utils.MoieExitCodes
import de.thm.moie.Global.ApplicationMode

import scala.concurrent.{Future, blocking}
import scala.io.StdIn

class Server()
    extends Routes
    with ServerSetup
    with ValidateConfig {

  override implicit lazy val actorSystem = ActorSystem("moie-system", akkaConfig)
  override implicit lazy val materializer = ActorMaterializer()
  override lazy val projectsManager: ActorRef = actorSystem.actorOf(Props[ProjectsManagerActor], name = "Root-ProjectsManager")

  val errors = validateConfig(Global.config)
  if(!Global.configDidExist) {
    serverlog.error(s"""Your configuration (${Global.configFileURL}) got newly created!
                        |Please adjust the settings before continuing.""".stripMargin)
    MoieExitCodes.waitAndExit(MoieExitCodes.UNMODIFIED_CONFIG)
  } else if(errors.nonEmpty) {
    val errorString = errors.map(x => s" - $x").mkString("\n")
    serverlog.error(s"""Your configuration (${Global.configFileURL}) contains the following errors:
                        |$errorString""".stripMargin)
    MoieExitCodes.waitAndExit(MoieExitCodes.CONFIG_ERROR)
  }

  val bindingFuture =
    Http().bindAndHandle(routes, interface, port)

  bindingFuture.onFailure {
    case ex:Exception =>
      serverlog.error("Failed to start server at {}:{} - {}", interface, port, ex.getMessage)
      actorSystem.terminate()
  }

  bindingFuture.onSuccess { case _ => serverlog.info("Server running at {}:{}", interface, port) }

  if(applicationMode == ApplicationMode.Development) {
    Future {
      blocking {
        serverlog.info("Press Enter to interrupt")
        StdIn.readLine()
        bindingFuture.
          flatMap(_.unbind()).
          onComplete(_ => actorSystem.terminate())
      }
    }
  }
}
