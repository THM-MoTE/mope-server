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

package de.thm.moie.server

import java.nio.file.WatchEvent.Kind
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.{BiConsumer, Predicate}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.pipe
import de.thm.moie.project.InternalProjectConfig
import de.thm.moie.utils.actors.UnhandledReceiver
import ews.EnhancedWatchService

import scala.concurrent.Future

class FileWatchingActor(interestee:ActorRef, rootPath:Path, outputDirName:String)(implicit projConfig:InternalProjectConfig)
    extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import FileWatchingActor._
  import context.dispatcher

  private val eventKinds = Seq(
    StandardWatchEventKinds.ENTRY_CREATE,
    StandardWatchEventKinds.ENTRY_DELETE
  )

  private val callback = new BiConsumer[Path, WatchEvent.Kind[_]] {
    override def accept(path: Path, kind: Kind[_]): Unit = kind match {
      case StandardWatchEventKinds.ENTRY_CREATE =>
        interestee ! NewPath(path)
      case StandardWatchEventKinds.ENTRY_DELETE =>
        interestee ! DeletedPath(path)
    }
  }

  private val dirFilter = new Predicate[Path] {
    override def test(dir: Path): Boolean =
      !Files.isHidden(dir) &&
      dir.getFileName.toString != outputDirName
  }

  private val modelicaFileFilter = new Predicate[Path] {
    override def test(file: Path): Boolean =
      if(Files.isDirectory(file)) dirFilter.test(file)
      else moFileFilter(file)
  }

  private val watchService = new EnhancedWatchService(rootPath, true, eventKinds:_*)
  private val runningFuture = watchService.start(projConfig.blockingExecutor,callback, dirFilter, modelicaFileFilter)

  private def files(path:Path) =
    getFiles(path, moFileFilter).sorted

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
  sealed trait FileWatchingMsg
  case object GetFiles extends FileWatchingMsg
  case class GetFiles(root:Path) extends FileWatchingMsg
  case class NewFile(path:Path) extends FileWatchingMsg
  case class NewDir(path:Path) extends FileWatchingMsg
  case class DeleteFile(path:Path) extends FileWatchingMsg

  case class NewPath(path:Path)
  case class DeletedPath(path:Path)
  case class ModifiedPath(path:Path)

  type PathFilter = Path => Boolean

  def moFileFilter(path:Path):Boolean = {
    !Files.isHidden(path) &&
      path.toString.endsWith(".mo")
  }

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
