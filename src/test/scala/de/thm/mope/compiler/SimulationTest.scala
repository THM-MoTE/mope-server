package de.thm.mope.compiler

import java.nio.file.Files

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.typesafe.config.{Config, ConfigFactory}
import de.thm.mope.config.ProjectConfig
import de.thm.mope.project.ProjectDescription
import de.thm.mope.server.FileWatchingActor
import de.thm.mope.tree.FileSystemTree
import de.thm.mope.{ActorSpec, MopeModule, PathFilter, TestHelpers}
import org.scalatest.Inspectors

import scala.util.Success

class SimulationTest extends ActorSpec with Inspectors {
  val outer = this
  val module = new MopeModule {
    override def config: Config = outer.config
    override implicit def actorSystem: ActorSystem = outer.system
    override implicit def mat: ActorMaterializer = ActorMaterializer()(actorSystem)
  }
  val tmpDir = Files.createTempDirectory("mope-sim-tests")
  val modelicaFile = TestHelpers.createFile("/BouncingBall.mo", tmpDir.resolve("BouncingBall.mo"))

  val descr = ProjectDescription(tmpDir.toString, tmpDir.resolve("out").toString, None)
  val compiler = new OMCompiler(ProjectConfig(module.serverConfig, descr))

  "The OMCompiler#'simulate' function" should {
    "simulate the bouncing ball without errors" in {
      compiler.compile(projectTree(tmpDir, descr.outputDirectory), modelicaFile)
      compiler.simulate("BouncingBall", Map()) shouldBe an [Success[_]]
    }
    "return a map of variables" in {
      compiler.compile(projectTree(tmpDir, descr.outputDirectory), modelicaFile)
      val map = compiler.simulate("BouncingBall", Map("stopTime" -> "3")).get
      println(map)
      //all the same size
      val size = map.head._2.size
      forAll(map.values) { xs =>
        xs should have size(size)
      }
      //contain at least h,v & time
      forAll(Seq("h", "v", "time")) { k =>
        map.get(k) shouldBe an [Some[_]]
      }
    }
  }
}
