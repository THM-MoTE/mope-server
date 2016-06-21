/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file.WatchEvent.Kind
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.{Executors, TimeUnit}
import java.util.function.{BiConsumer, Predicate}

import akka.actor.{Actor, ActorRef}
import akka.pattern.pipe
import de.thm.moie.utils.ResourceUtils
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.concurrent.Future
import ews.EnhancedWatchService

import scala.collection.mutable

class FileWatchingActor(interestee:ActorRef, rootPath:Path, outputDirName:String)
    extends Actor
    with UnhandledReceiver
    with LogMessages {

  import FileWatchingActor._
  import context.dispatcher

  private val eventKinds = Seq(
    StandardWatchEventKinds.ENTRY_CREATE,
    StandardWatchEventKinds.ENTRY_DELETE
  )

  private val executor = Executors.newSingleThreadExecutor()

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
      dirFilter.test(file) || moFileFilter(file)
  }

  private val watchService = new EnhancedWatchService(rootPath, true, eventKinds:_*)
  private val runningFuture = watchService.start(executor,callback, dirFilter, modelicaFileFilter)

  private def files(path:Path) =
    getFiles(path, moFileFilter).sorted

  private def moFileFilter(path:Path):Boolean = {
    !Files.isHidden(path) &&
    path.toString.endsWith(".mo")
  }

  override def handleMsg: Receive = {
    case GetFiles =>
      Future(files(rootPath)) pipeTo sender
    case GetFiles(root) =>
      Future(files(root)) pipeTo sender
  }

  override def postStop(): Unit = {
    runningFuture.cancel(true)
    executor.shutdown()
    executor.awaitTermination(2, TimeUnit.SECONDS)
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
