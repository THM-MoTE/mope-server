package de.thm.moie.server

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._

import akka.pattern.pipe
import akka.actor.{Actor, ActorLogging, ActorRef}
import de.thm.moie.compiler.{AsyncModelicaCompiler, ModelicaCompiler}
import de.thm.moie.project.ProjectDescription
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.collection.mutable.ArrayBuffer

class ProjectManagerActor(description:ProjectDescription,
                          compiler:ModelicaCompiler with AsyncModelicaCompiler)
  extends Actor
  with UnhandledReceiver
  with LogMessages {

  import ProjectManagerActor._

  //initialize all files
  val rootDir = Paths.get(description.path)
  val files = getModelicaFiles(rootDir)
  println("Projects-Files: \n" + files.mkString("\n"))

  override def handleMsg: Receive = {
    case CompilerProject => println("compiling not implemented yet")
  }

  override def postStop(): Unit = {
    log.info("stopping")
  }
}

object ProjectManagerActor {
  sealed trait ProjectManagerMsg
  case object CompilerProject extends ProjectManagerMsg

  def getModelicaFiles(root:Path): List[Path] = {
    val visitor = new AccumulateFiles()
    Files.walkFileTree(root, visitor)
    visitor.getFiles
  }

  private class AccumulateFiles extends SimpleFileVisitor[Path] {
    private var buffer = List[Path]()
    override def visitFile(file:Path,
                           attr:BasicFileAttributes): FileVisitResult = {
      if(attr.isRegularFile && !Files.isHidden(file))
        buffer = file :: buffer

      FileVisitResult.CONTINUE
    }

    def getFiles = buffer
  }
}
