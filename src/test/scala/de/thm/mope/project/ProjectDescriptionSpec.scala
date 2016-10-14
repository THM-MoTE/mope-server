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

package de.thm.mope.project

import java.nio.file._

import de.thm.mope._
import org.scalatest._

class ProjectDescriptionSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  val path = Files.createTempDirectory("moie")
  val scriptPath = path.resolve("build.mos")

  Files.createFile(scriptPath)

  override def afterAll = {
    removeDirectoryTree(path)
  }

  "ProjectDescription.validate" should "return errors for undefined path" in {
    val descr = ProjectDescription("unknown-path", "target", None)

    val errors = ProjectDescription.validate(descr)
    errors should have size 1
    errors.head should be ("`unknown-path` doesn't exist")
  }

  it should "return errors for undefined buildScript" in {
    val descr = ProjectDescription(path.toString,
                                    "target",
                                    Some("unknownScript"))

    val errors = ProjectDescription.validate(descr)
    errors should have size 1
    errors.head should be (s"${path.resolve("unknownScript")} isn't a regular *.mos file!")

    val descr2 = ProjectDescription(path.toString,
                                    "target",
                                    None)

    val errors2 = ProjectDescription.validate(descr2)
    errors2 should have size 0
  }

  it should "return no errors for valid description" in {
    val descr = ProjectDescription(System.getProperty("user.home"),
                                    "target",
                                    None)

    val errors = ProjectDescription.validate(descr)
    errors should have size 0

    val descr2 = ProjectDescription(path.toString,
                                    "target",
                                    Some(scriptPath.getFileName.toString))

    val errors2 = ProjectDescription.validate(descr2)
    errors2 should have size 0
  }
}
