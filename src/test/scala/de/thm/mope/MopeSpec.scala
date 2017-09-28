package de.thm.mope

import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest._
import org.scalatest.time._

import scala.language.postfixOps
import scala.concurrent.duration._

trait MopeSpec
  extends WordSpecLike
    with Matchers
    with ScalaFutures
    with Inspectors {
  implicit lazy val defaultTimeout = Timeout(5 seconds)
  implicit override val patienceConfig = PatienceConfig(timeout = Span(20, Seconds), interval = Span(20, Millis))
}
