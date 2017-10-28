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

package de.thm.mope.server

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ExecutorService

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.pipe
import de.thm.mope.utils.actors.UnhandledReceiver
import ews._

import scala.concurrent.Future

class FileWatchingActor(interestee: ActorRef, rootPath: Path, outputDirName: String, executor: ExecutorService)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import FileWatchingActor._
  import context.dispatcher

  private val eventKinds = Seq(
    StandardWatchEventKinds.ENTRY_CREATE,
    StandardWatchEventKinds.ENTRY_DELETE
  )

  private val listener = new WatchServiceListener() {
    override def created(p: Path): Unit = {
      interestee ! NewPath(p)
    }

    override def deleted(p: Path): Unit = {
      interestee ! DeletedPath(p)
    }

    override def modified(p: Path): Unit = ()
  }

  private val filter = new ews.PathFilter() {
    override def acceptDirectory(dir: Path): Boolean =
      !Files.isHidden(dir) && dir.getFileName.toString != outputDirName

    override def acceptFile(file: Path): Boolean =
      if (Files.isDirectory(file)) acceptDirectory(file)
      else moFileFilter(file)
  }

  private val watchService = new EnhancedWatchService(rootPath, true, eventKinds: _*)

  private val runningFuture = executor.submit(watchService.setup(listener, filter))

  private def files(path: Path) =
    getFiles(path, filter.acceptFile).sorted

  override def receive: Receive = {
    case GetFiles =>
      Future(files(rootPath)) pipeTo sender
    case GetFiles(root) =>
      Future(files(root)) pipeTo sender
  }

  override def postStop(): Unit = {
    runningFuture.cancel(true)
    log.info("stopping")
  }
}

object FileWatchingActor {

  import de.thm._

  sealed trait FileWatchingMsg

  case object GetFiles extends FileWatchingMsg

  case class GetFiles(root: Path) extends FileWatchingMsg

  case class NewFile(path: Path) extends FileWatchingMsg

  case class NewDir(path: Path) extends FileWatchingMsg

  case class DeleteFile(path: Path) extends FileWatchingMsg

  case class NewPath(path: Path)

  case class DeletedPath(path: Path)

  case class ModifiedPath(path: Path)

  def moFileFilter(path: Path): Boolean = {
    !Files.isHidden(path) &&
      path.toString.endsWith(".mo")
  }

  def getFiles(root: Path, filter: mope.PathFilter): List[Path] = {
    val visitor = new AccumulateFiles(filter)
    Files.walkFileTree(root, visitor)
    visitor.getFiles
  }

  def getDirs(path: Path, filter: mope.PathFilter): List[Path] = {
    val visitor = new AccumulateDirs(filter)
    Files.walkFileTree(path, visitor)
    visitor.getDirs
  }

  private class AccumulateFiles(filter: mope.PathFilter) extends SimpleFileVisitor[Path] {
    private var buffer = List[Path]()

    override def visitFile(file: Path,
                           attr: BasicFileAttributes): FileVisitResult = {
      if (filter(file)) {
        buffer = file :: buffer
      }

      FileVisitResult.CONTINUE
    }

    def getFiles = buffer
  }

  private class AccumulateDirs(filter: mope.PathFilter) extends SimpleFileVisitor[Path] {
    private var buffer = List[Path]()

    override def preVisitDirectory(dir: Path,
                                   attrs: BasicFileAttributes): FileVisitResult = {
      if (!Files.isHidden(dir) && filter(dir)) {
        buffer = dir :: buffer
        FileVisitResult.CONTINUE
      } else FileVisitResult.SKIP_SUBTREE
    }

    def getDirs = buffer
  }

}
