/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.event.{Logging, LogSource}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.language.postfixOps

import de.thm.moie.Global

trait ServerSetup {
  val serverName = "moie-server"
  val applicationMode = Global.ApplicationMode.parseString(Global.config.getString("app.mode").getOrElse("prod"))

  implicit val serverLogSource:LogSource[ServerSetup] = new LogSource[ServerSetup] {
    override def genString(setup:ServerSetup): String =
      setup.serverName
  }

  private val akkaConfig = ConfigFactory.parseFile(
    new java.io.File(Global.configFileURL.toURI))

  implicit val actorSystem = ActorSystem("moie-system", akkaConfig)
  implicit val execContext = actorSystem.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val defaultTimeout = Timeout(20 seconds)

  val serverlog = Logging(actorSystem, this)
}
