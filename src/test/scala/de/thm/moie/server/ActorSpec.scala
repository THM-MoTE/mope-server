package de.thm.moie.server

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.{ TestActors, TestKit, ImplicitSender, TestProbe }
import akka.actor.ActorSystem

abstract class ActorSpec extends TestKit(ActorSystem("specSystem"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
