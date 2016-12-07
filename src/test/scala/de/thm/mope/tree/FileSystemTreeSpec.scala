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

package de.thm.mope.tree

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.scalatest.Inspectors._
import java.nio.file._

import de.thm.mope._

class FileSystemTreeSpec extends WordSpec with Matchers with BeforeAndAfterAll {
	import FileSystemTree._
	val path = Files.createTempDirectory("moie")
	val projectPath = path.resolve("mo-project")
	val emptyPath = path.resolve("empty")

	val files = List(
			// moie/mo-project/*
		projectPath.resolve("test.mo"),
		projectPath.resolve("test.txt"),
		projectPath.resolve("model.mo"),
		projectPath.resolve("model.test"),
		projectPath.resolve("util/model2.mo"),
		projectPath.resolve("util/resistor.mo"),
		projectPath.resolve("common/transistor.mo"),
		projectPath.resolve("common/test/algorithm.mo"),
		projectPath.resolve("common/test/algorithm.java"),
			// moie/empty/*
		emptyPath.resolve("test/test.txt"),
		emptyPath.resolve("test.txt"),
		emptyPath.resolve("rechnung.java"),
		emptyPath.resolve("common/uebung.java")
	)

	val dirs = List(
		projectPath.resolve("util"),
		projectPath.resolve("common/test"),
		emptyPath.resolve("test"),
		emptyPath.resolve("common")
	)

	override def beforeAll(): Unit = {
		//create tmp Files
		dirs.foreach(Files.createDirectories(_))
		files.foreach(Files.createFile(_))
	}
	override def afterAll(): Unit = {
		removeDirectoryTree(path)
	}

	"A FSTree" should {
		"built a tree from a filesystem" in {
			val tree = FileSystemTree(path)
			tree should not be null
		}
		"contain all directories of a filesystem tree" in {
			val tree = FileSystemTree(path)
			forAll(dirs) { dir =>
				tree.contains(dir) shouldBe true
			}
		}

		"contain all files of a filesystem tree" in {
			val tree = FileSystemTree(path)
			forAll(files) { file =>
				tree.contains(file) shouldBe true
			}
		}
		"only contain files & directories from a filesystem tree" in {
			val tree = FileSystemTree(path)
			tree.size shouldBe 21
		}
		"add a path that is a file" in {
			/**
				* /h/nico/Documents
				* 	- /h/nico/Documents/tst
				* 		-	/h/nico/Documents/tst/1.txt
				*/
			val tree = Node(Paths.get("/h/nico/Documents"),
				List(Node(Paths.get("/h/nico/Documents/tst"),
					List(Leaf(Paths.get("/h/nico/Documents/tst/1.txt"))))
				))
/*
			/**
				* /h/nico/Documents
				* 	- /h/nico/Documents/tst
				* 		-	/h/nico/Documents/tst/2.txt <= added
				* 		-	/h/nico/Documents/tst/1.txt
				*/
			add(tree)(Paths.get("/h/nico/Documents/tst/2.txt")) shouldBe Node(Paths.get("/h/nico/Documents"),
									List(Node(Paths.get("/h/nico/Documents/tst"),
										List(Leaf(Paths.get("/h/nico/Documents/tst/2.txt")), Leaf(Paths.get("/h/nico/Documents/tst/1.txt"))))
									))

			/**
				* /h/nico/Documents
				* 	- /h/nico/Documents/t.txt <= added
				* 	- /h/nico/Documents/tst
				* 		-	/h/nico/Documents/tst/1.txt
				*/
			add(tree)(Paths.get("/h/nico/Documents/t.txt")) shouldBe Node(Paths.get("/h/nico/Documents"),
								List(Leaf(Paths.get("/h/nico/Documents/t.txt")),
									Node(Paths.get("/h/nico/Documents/tst"),
									List(Leaf(Paths.get("/h/nico/Documents/tst/1.txt"))))
								))
*/
			/**
				* /h/nico/Documents
				* 	- /h/nico/Documents/t <= added
				* 	- /h/nico/Documents/tst
				* 		-	/h/nico/Documents/tst/1.txt
				*/
			val tree2 = add(tree)(Paths.get("/h/nico/Documents/t"))
			/**
				* /h/nico/Documents
				* 	- /h/nico/Documents/t
				* 		-	/h/nico/Documents/t/x.txt <= added
				* 	- /h/nico/Documents/tst
				* 		-	/h/nico/Documents/tst/1.txt
				*/
			add(tree2)(Paths.get("/h/nico/Documents/t/x.txt")) shouldBe Node(Paths.get("/h/nico/Documents"),
				List(Node(Paths.get("/h/nico/Documents/t"), List(Leaf(Paths.get("/h/nico/Documents/t/x.txt")))),
					Node(Paths.get("/h/nico/Documents/tst"),
						List(Leaf(Paths.get("/h/nico/Documents/tst/1.txt"))))
				))
		}
	}
}
