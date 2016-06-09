/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie

import akka.actor.ActorSystem
import akka.actor.{ActorRef}
import akka.actor.Props
import akka.util.Timeout
import akka.testkit.{ TestActors, TestKit, ImplicitSender, TestProbe }
import scala.concurrent.duration._
import scala.language.postfixOps

package object server {
    val defaultTime = 5 seconds
    implicit val defaultTimeout = Timeout(defaultTime)
}
