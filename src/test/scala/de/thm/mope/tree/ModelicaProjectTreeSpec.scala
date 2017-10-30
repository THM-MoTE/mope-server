/**
  * Copyright (C) 2016,2017 Nicola Justus <nicola.justus@mni.thm.de>
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


package de.thm.mope.tree
import java.nio.file._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.scalatest.Inspectors._

import de.thm.mope.TestHelpers

class ModelicaProjectTreeSpec extends WordSpec with Matchers with BeforeAndAfterAll {
	import ModelicaProjectTree._

	val projectPath = Files.createTempDirectory("moie")

	val files = List(
		projectPath.resolve("test.mo"),
		projectPath.resolve("test.txt"),
		projectPath.resolve("model.mo"),
		projectPath.resolve("model.test"),
			//moie/util/*
		projectPath.resolve(s"util/$packageFilename"),
		projectPath.resolve("util/model2.mo"),
		projectPath.resolve("util/resistor.mo"),
			//moie/test/*
		projectPath.resolve(s"test/$packageFilename"),
		projectPath.resolve("test/resis.mo"),
		projectPath.resolve(s"test/t2/$packageFilename"),
		projectPath.resolve("test/t2/file.mo"),
		projectPath.resolve("test/t2/file2.mo"),
		projectPath.resolve("test/t5/Model.mo"),
		projectPath.resolve("test/t5/Model2.mo"),
		projectPath.resolve("common/file.mo"),
		projectPath.resolve("common/DS_Store")
	)

	val dirs = List(
		projectPath.resolve("util"),
		projectPath.resolve("common"),
		projectPath.resolve("test/t2"),
		projectPath.resolve("test/t5")
	)

	override def beforeAll(): Unit = {
		//create tmp Files
		dirs.foreach(Files.createDirectories(_))
		files.foreach(Files.createFile(_))
	}
	override def afterAll(): Unit = {
		TestHelpers.removeDirectoryTree(projectPath)
	}

	"The method 'packageMoDirectories'" should {
		s"return all path's to '$packageFilename' files" in {
			val tree = FileSystemTree(projectPath)
			packageMoDirectories(tree).toSet shouldBe Set(projectPath.resolve("util"),
								projectPath.resolve("test"))
		}
		"return only directories" in {
			val tree = FileSystemTree(projectPath)
			forAll(packageMoDirectories(tree)) { path =>
				Files.isDirectory(path) shouldBe true
			}
		}
	}
	"The method 'singeFiles'" should {
		s"return only files that aren't inside of a '$packageFilename' organized directory" in {
			val tree = FileSystemTree(projectPath)
			val packageDirectories = Set(projectPath.resolve("util"),
								projectPath.resolve("test"))
			forAll(singleFiles(tree)) { path =>
				forAll(packageDirectories) { pckDir =>
					!path.startsWith(pckDir)
				}
			}
		}
		"return only *.mo files" in {
			val tree = FileSystemTree(projectPath)
			val pathes = singleFiles(tree)
			forAll(pathes) { path =>
				path.endsWith(".mo")
			}
		}
		"return only files that aren't inside of a 'package.mo' directory" in {
			val tree = FileSystemTree(projectPath)
			val packageDirectories = Set(projectPath.resolve("util"),
								projectPath.resolve("test"))
			forAll(singleFiles(tree)) { path =>
				Files.isRegularFile(path) &&
				!packageDirectories.exists(path.startsWith)
			}
		}
	}
}
