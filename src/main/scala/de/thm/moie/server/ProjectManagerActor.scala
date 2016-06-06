/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import java.util.concurrent.Executors
import scala.collection._

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
  import context.dispatcher

  val blockingExecutor = Executors.newCachedThreadPool()

  //initialize all files
  val rootDir = Paths.get(description.path)
  val files = mutable.ArrayBuffer[Path]()
  val startedWatchers = mutable.ArrayBuffer[(FileWatcher,java.util.concurrent.Future[_])]()
  newWatcher(rootDir)

  def fileFilter(p:Path):Boolean = {
    val filename = ResourceUtils.getFilename(p)
    filename != description.outputDirectory &&
    !Files.isHidden(p) &&
    Files.isDirectory(p) ||
    (!Files.isDirectory(p) && filename.endsWith(".mo"))
  }

  def newWatcher(path:Path):Unit = {
    val dirs = getDirs(path, { p =>
      ResourceUtils.getFilename(p) != description.outputDirectory
    })
    files ++= getModelicaFiles(path, "mo")
    dirs.map { p =>
      val watcher = new FileWatcher(p, self)(fileFilter)
      val future = blockingExecutor.submit(watcher)
      log.debug(s"start watcher for ${rootDir.relativize(p)}")
      startedWatchers += watcher -> future
      watcher
    }
  }

  if(files.nonEmpty)
    log.debug("Project-Files: \n" + files.mkString("\n"))

  override def handleMsg: Receive = {
    case CompileProject => compiler.compileAsync(files.toList) pipeTo sender
    case NewFile(path) =>
      files += path
    case NewDir(path) => newWatcher(path)
    case DeleteFile(path) => files -= path
  }

  override def postStop(): Unit = {
    log.info("stopping")
    startedWatchers.map(_._2).foreach(_.cancel(true))
    blockingExecutor.shutdown()
  }
}

object ProjectManagerActor {
  sealed trait ProjectManagerMsg
  case object CompileProject extends ProjectManagerMsg
  case class NewFile(path:Path) extends ProjectManagerMsg
  case class NewDir(path:Path) extends ProjectManagerMsg
  case class DeleteFile(path:Path) extends ProjectManagerMsg

  def getModelicaFiles(root:Path, filters:String*): List[Path] = {
    val visitor = new AccumulateFiles(filters)
    Files.walkFileTree(root, visitor)
    visitor.getFiles
  }

  def getDirs(path:Path, filter: Path => Boolean): List[Path] = {
    val visitor = new AccumulateDirs(filter)
    Files.walkFileTree(path, visitor)
    visitor.getDirs
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

  private class AccumulateDirs(filter: Path => Boolean) extends SimpleFileVisitor[Path] {
    private var buffer = List[Path]()

    override def preVisitDirectory(dir:Path,
                                  attrs:BasicFileAttributes): FileVisitResult = {
      if(!Files.isHidden(dir) && filter(dir)) {
        buffer = dir :: buffer
        FileVisitResult.CONTINUE
      } else FileVisitResult.SKIP_SUBTREE
    }

    def getDirs = buffer
  }
}
