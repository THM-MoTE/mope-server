/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import akka.actor.Actor
import akka.pattern.pipe
import de.thm.moie.utils.ResourceUtils
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.concurrent.Future

class FileWatchingActor(rootPath:Path, outputDirName:String)
    extends Actor
    with UnhandledReceiver
    with LogMessages {

  import FileWatchingActor._
  import context.dispatcher

  private def files = getFiles(rootPath, moFileFilter).sorted

  private def moFileFilter(path:Path):Boolean = {
    val filename = ResourceUtils.getFilename(path)
    Files.isRegularFile(path) &&
    !Files.isHidden(path) &&
    filename.endsWith(".mo")
  }

  override def handleMsg: Receive = {
    case GetFiles =>
      Future(files) pipeTo sender
  }

  override def postStop(): Unit = {
    log.info("stopping")
  }
}

object FileWatchingActor {
  sealed trait FileWatchingMsg
  case object GetFiles extends FileWatchingMsg
  case class NewFile(path:Path) extends FileWatchingMsg
  case class NewDir(path:Path) extends FileWatchingMsg
  case class DeleteFile(path:Path) extends FileWatchingMsg


  type PathFilter = Path => Boolean

  def getFiles(root:Path, filter:PathFilter): List[Path] = {
    val visitor = new AccumulateFiles(filter)
    Files.walkFileTree(root, visitor)
    visitor.getFiles
  }

  def getDirs(path:Path, filter: PathFilter): List[Path] = {
    val visitor = new AccumulateDirs(filter)
    Files.walkFileTree(path, visitor)
    visitor.getDirs
  }

  private class AccumulateFiles(filter:PathFilter) extends SimpleFileVisitor[Path] {
    private var buffer = List[Path]()
    override def visitFile(file:Path,
                           attr:BasicFileAttributes): FileVisitResult = {
      if(filter(file)) {
        buffer = file :: buffer
      }

      FileVisitResult.CONTINUE
    }

    def getFiles = buffer
  }

  private class AccumulateDirs(filter:PathFilter) extends SimpleFileVisitor[Path] {
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
