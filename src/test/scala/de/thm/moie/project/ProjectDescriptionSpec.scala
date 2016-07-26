package de.thm.moie.project

import java.nio.file._

import de.thm.moie._
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
    errors.head should be ("unknown-path isn't a directory")
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
