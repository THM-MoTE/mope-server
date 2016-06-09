/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.io.StdIn
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes

import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.sys.process._
import scala.concurrent.Future
import scala.concurrent.blocking
import java.io._

import de.thm.moie.Global.ApplicationMode
import de.thm.moie.project.ProjectDescription

class Server(port:Int) extends Routes with ServerSetup {

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
