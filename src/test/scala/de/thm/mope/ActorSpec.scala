package de.thm.mope

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

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.{Config, ConfigFactory}
import de.thm.mope.config._
import de.thm.mope.server.FileWatchingActor
import de.thm.mope.tree.FileSystemTree
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import java.nio.file.{Files, Path}

abstract class ActorSpec(val config:Config = ConfigProvider.resourceProvidedConfig) extends TestKit(ActorSystem("specSystem", config))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {
    override def afterAll = {
      TestKit.shutdownActorSystem(system)
    }

  def treeFilter(outputDir:String): PathFilter = { p =>
    Files.isDirectory(p) || FileWatchingActor.moFileFilter(p) ||
    p.endsWith(outputDir)
  }

  def projectTree(p:Path, outputDir:String) = FileSystemTree(p, treeFilter(outputDir))
}
