/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.language.postfixOps

import de.thm.moie.Global

trait ServerSetup {

  private val akkaConfig = ConfigFactory.parseFile(
    new java.io.File(Global.configFileURL.toURI))

  implicit val actorSystem = ActorSystem("moie-system", akkaConfig)
  implicit val execContext = actorSystem.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val defaultTimeout = Timeout(20 seconds)
}
