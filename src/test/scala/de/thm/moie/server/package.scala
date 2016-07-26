/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie

import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

package object server {
  object timeouts {
    val defaultTime = 5 seconds
    implicit val defaultTimeout = Timeout(defaultTime)
  }
}
