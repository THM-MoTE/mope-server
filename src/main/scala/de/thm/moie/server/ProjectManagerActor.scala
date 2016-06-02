package de.thm.moie.server

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._

import akka.pattern.pipe
import akka.actor.Actor
import de.thm.moie.compiler.ModelicaCompiler
import de.thm.moie.project.ProjectDescription
import de.thm.moie.utils.ResourceUtils
import de.thm.moie.utils.actors.UnhandledReceiver

class ProjectManagerActor(description:ProjectDescription,
                          compiler:ModelicaCompiler)
  extends Actor
  with UnhandledReceiver
  with LogMessages {

  import ProjectManagerActor._

  //initialize all files
  val rootDir = Paths.get(description.path)
  val files = getModelicaFiles(rootDir, "mo")
  println("Projects-Files: \n" + files.mkString("\n"))

  override def handleMsg: Receive = {
    case CompileProject => println("compiling not implemented yet")
  }

  override def postStop(): Unit = {
    log.info("stopping")
  }
}

object ProjectManagerActor {
  sealed trait ProjectManagerMsg
  case object CompileProject extends ProjectManagerMsg

  def getModelicaFiles(root:Path, filters:String*): List[Path] = {
    val visitor = new AccumulateFiles(filters)
    Files.walkFileTree(root, visitor)
    visitor.getFiles
  }

  private class AccumulateFiles(filters:Seq[String]) extends SimpleFileVisitor[Path] {
    private var buffer = List[Path]()
    override def visitFile(file:Path,
                           attr:BasicFileAttributes): FileVisitResult = {
      if(attr.isRegularFile &&
        !Files.isHidden(file) &&
        filters.exists(ResourceUtils.getFilename(file).endsWith(_))) {
        buffer = file :: buffer
      }

      FileVisitResult.CONTINUE
    }

    def getFiles = buffer
  }
}
