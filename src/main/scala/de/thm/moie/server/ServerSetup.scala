/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import de.thm.moie.Global._

import scala.concurrent.duration._
import scala.language.postfixOps

trait ServerSetup {

  /* Route java.util.logging into slf4j */
 // remove existing handlers attached to j.u.l root logger
 org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger()
 // add SLF4JBridgeHandler to j.u.l's root logger
 org.slf4j.bridge.SLF4JBridgeHandler.install()

  val serverName = "moie-server"
  val applicationMode = ApplicationMode.parseString(config.getString("app.mode"))

  implicit val serverLogSource:LogSource[ServerSetup] = new LogSource[ServerSetup] {
    override def genString(setup:ServerSetup): String =
      setup.serverName
  }

  val akkaConfig = ConfigFactory.parseFile(
    new java.io.File(configFileURL.toURI))

  /** Overwrite as lazy val! */
  implicit def actorSystem:ActorSystem
  /** Overwrite as lazy val! */
  implicit def execContext = actorSystem.dispatcher
  /** Overwrite as lazy val! */
  implicit def materializer:ActorMaterializer
  implicit val defaultTimeout = Timeout(config.getInt("defaultAskTimeout") seconds)

  val serverlog = Logging(actorSystem, this)
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")
}
