/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.ActorRef
import java.nio.file.{ Files, WatchEvent }
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds._
import java.util.concurrent.TimeUnit
import de.thm.moie.Global
import de.thm.moie.utils.ThreadUtils

import scala.collection.JavaConversions._

class FileWatcher(rootDir:Path, observer:ActorRef)(fileFilter: Path => Boolean) extends Runnable {
  val pollingTimeout =  Global.config.getInt("filewatcher-polling-timeout").getOrElse(10)

  import de.thm.moie.server.ProjectManagerActor._

  def run(): Unit = ThreadUtils.faileSafeRun {
    val watcher = rootDir.getFileSystem.newWatchService()
    rootDir.register(watcher, ENTRY_CREATE, ENTRY_DELETE)
    while(!Thread.currentThread.isInterrupted() && Files.exists(rootDir)) {
      val key = watcher.poll(pollingTimeout, TimeUnit.SECONDS)
      if(key != null) {
        key.pollEvents.collect {
          case ev:WatchEvent[_] if(ev.kind != OVERFLOW) => (ev.context.asInstanceOf[Path], ev.kind)
        }.foreach { case (path, kind) =>
            val absolutePath = rootDir.resolve(path).toAbsolutePath()
            if(kind == ENTRY_CREATE && fileFilter(absolutePath)) {
              if(Files.isDirectory(absolutePath))
                observer ! NewDir(absolutePath)
              else
                observer ! NewFile(absolutePath)
            } else if(kind == ENTRY_DELETE && fileFilter(absolutePath)) {
              if(Files.isRegularFile(absolutePath)) {
                observer ! DeleteFile(absolutePath)
              }
            }
        }
        if(!key.reset())
          Thread.currentThread().interrupt()
      }
    }
    watcher.close()
  }
}
