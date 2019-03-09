/**
  * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */

package de.thm.mope.server

import java.nio.file.Files
import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.pattern.pipe
import akka.testkit.TestActorRef
import com.typesafe.config.{Config, ConfigFactory}
import de.thm.mope.project.ProjectDescription
import de.thm.mope.compiler.{CompilerError, ModelicaCompiler}
import de.thm.mope.models.SimulationResult
import de.thm.mope.config.ProjectConfig
import de.thm.mope.compiler._
import de.thm.mope.{ActorSpec, MopeModule, PathFilter, TestHelpers}
import org.scalatest.Inspectors
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class SimulateActorTest  extends ActorSpec with Inspectors {
  val outer = this
  val module = new MopeModule {
    override def config: Config = outer.config
    override implicit def actorSystem: ActorSystem = outer.system
    override implicit def mat: ActorMaterializer = ActorMaterializer()(actorSystem)
  }
  import module.serverConfig._
  val tmpDir = Files.createTempDirectory("mope-sim-tests")
  val modelicaFile = TestHelpers.createFile("/BouncingBall.mo", tmpDir.resolve("BouncingBall.mo"))

  val descr = ProjectDescription(tmpDir.toString, tmpDir.resolve("out").toString, None)
  val conf = ProjectConfig(module.serverConfig, descr)
  val compiler = new OMCompiler(conf)

  val testRef = TestActorRef(new SimulateActor(compiler, conf))
  val underlying = testRef.underlyingActor

  "The SimulateActor" should {
    compiler.compile(projectTree(tmpDir, descr.outputDirectory), modelicaFile)
    "provide an id for workers" in {
      testRef ! SimulateActor.SimulateModel("BouncingBall", Map("stopTime" -> "3"))
      val sid:SimulateActor.SimulationId = expectMsgType[SimulateActor.SimulationId](5 seconds)
      Await.result(underlying.context.actorSelection(sid.id).resolveOne, 5 seconds) shouldBe an [ActorRef]
      testRef ! sid
      expectMsgType[Either[SimulateActor.NotFinished,SimulationResult]](5 seconds) shouldBe an [Left[_,_]]
      Thread.sleep(8000)
      testRef ! sid
      expectMsgType[Either[SimulateActor.NotFinished,SimulationResult]](5 seconds) shouldBe an [Right[_,_]]
    }
    "return 'NotFinished' if still running" in {
      testRef ! SimulateActor.SimulateModel("BouncingBall", Map("stopTime" -> "6"))
      val sid:SimulateActor.SimulationId = expectMsgType[SimulateActor.SimulationId](5 seconds)
      testRef ! sid
      val Left(x) = expectMsgType[Left[SimulateActor.NotFinished,SimulationResult]](5 seconds)
      x shouldBe an [SimulateActor.NotFinished]
      Thread.sleep(8000)
      testRef ! sid
      expectMsgType[Right[SimulateActor.NotFinished,SimulationResult]](5 seconds)
    }
  }
}
