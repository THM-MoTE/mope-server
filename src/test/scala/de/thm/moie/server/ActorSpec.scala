/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

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
  with BeforeAndAfterAll {
    override def afterAll = {
      TestKit.shutdownActorSystem(system)
    }
  }
