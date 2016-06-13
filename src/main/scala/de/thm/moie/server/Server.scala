/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.Http
import de.thm.moie.Global.ApplicationMode

import scala.concurrent.{Future, blocking}
import scala.io.StdIn

class Server() extends Routes with ServerSetup {

  private val escCode = 0x1b

  override lazy val projectsManager: ActorRef = actorSystem.actorOf(Props[ProjectsManagerActor], name = "Root-ProjectsManager")

  val bindingFuture = Http().bindAndHandle(routes, interface, port)
  serverlog.info(s"Server running at localhost:$port")
  if(applicationMode == ApplicationMode.Development) {
    Future {
      blocking {
        serverlog.info("Press Enter to interrupt")
        var char = StdIn.readLine()
        bindingFuture.
          flatMap(_.unbind()).
          onComplete(_ => actorSystem.terminate())
      }
    }
  }
}
