/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import java.util.concurrent.Executors
import scala.collection._

import akka.actor.Actor
import de.thm.moie.utils.ResourceUtils
import de.thm.moie.utils.actors.UnhandledReceiver

class FileWatchingActor(rootPath:Path, outputDirName:String)
    extends Actor
    with UnhandledReceiver
    with LogMessages {

  import FileWatchingActor._
  import context.dispatcher

  val blockingExecutor = Executors.newCachedThreadPool()
  val files = mutable.ArrayBuffer.concat(getFiles(rootPath, "mo"))
  val startedWatchers = mutable.ArrayBuffer[java.util.concurrent.Future[_]]()
  newWatcher(rootPath)

  def fileFilter(p:Path):Boolean = {
    val filename = ResourceUtils.getFilename(p)
    filename != outputDirName &&
    !Files.isHidden(p) &&
    Files.isDirectory(p) ||
    (!Files.isDirectory(p) && filename.endsWith(".mo"))
  }

  def newWatcher(path:Path):Unit = {
    val dirs = getDirs(path, { p =>
      ResourceUtils.getFilename(p) != outputDirName
    })
    files ++= getFiles(path, "mo")
    dirs.foreach { p =>
      val watcher = new FileWatcher(p, self)(fileFilter)
      val future = blockingExecutor.submit(watcher)
      log.debug(s"start watcher for ${rootPath.relativize(p)}")
      startedWatchers += future
    }
  }

  if(files.nonEmpty)
    log.debug("Project-Files: \n" + files.mkString("\n"))

  override def handleMsg: Receive = {
    case NewFile(path) =>
      files += path
      log.debug(s"file added: $path")
      log.debug(s"files: $files")
    case NewDir(path) =>
      newWatcher(path)
    case DeleteFile(path) => files -= path
  }

  override def postStop(): Unit = {
    log.info("stopping")
    startedWatchers.foreach(_.cancel(true))
    blockingExecutor.shutdown()
  }
}

object FileWatchingActor {
  sealed trait FileWatchingMsg
  case class NewFile(path:Path) extends FileWatchingMsg
  case class NewDir(path:Path) extends FileWatchingMsg
  case class DeleteFile(path:Path) extends FileWatchingMsg

  def getFiles(root:Path, filters:String*): List[Path] = {
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
