/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import de.thm.moie.Global.ApplicationMode

import scala.concurrent.{Future, blocking}
import scala.io.StdIn

class Server() extends Routes with ServerSetup {

  override implicit lazy val actorSystem = ActorSystem("moie-system", akkaConfig)
  override implicit lazy val materializer = ActorMaterializer()
  override lazy val projectsManager: ActorRef = actorSystem.actorOf(Props[ProjectsManagerActor], name = "Root-ProjectsManager")

  val bindingFuture = Http().bindAndHandle(routes, interface, port)
  serverlog.info("Server running at {}:{}", interface, port)
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
