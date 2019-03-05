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

import scala.util.Success

class SimulationTest extends ActorSpec {
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

  val treeFilter: PathFilter = { p =>
    Files.isDirectory(p) || FileWatchingActor.moFileFilter(p) ||
      p.endsWith(descr.outputDirectory)
  }

  "The OMCompiler" should {
    "simulate the bouncing ball without errors" in {
      compiler.compile(FileSystemTree(tmpDir, treeFilter), modelicaFile)
      compiler.simulate("BouncingBall", Map()) shouldBe an [Success[_]]
    }
  }
}
