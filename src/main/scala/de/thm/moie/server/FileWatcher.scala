/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.actor.ActorRef
import java.nio.file.{ FileSystems, Files, WatchEvent }
import java.nio.file.{Path, Paths}
import java.nio.file.StandardWatchEventKinds._
import java.util.concurrent.TimeUnit
import de.thm.moie.Global

import scala.collection.JavaConversions._

class FileWatcher(rootDir:Path, observer:ActorRef) extends Runnable {
  val pollingTimeout =  Global.config.getInt("filewatcher-polling-timeout").getOrElse(10)

  import de.thm.moie.server.ProjectManagerActor._

  def run(): Unit = {
    val watcher = rootDir.getFileSystem.newWatchService()
    rootDir.register(watcher, ENTRY_CREATE, ENTRY_DELETE)
    while(!Thread.currentThread.isInterrupted() && Files.exists(rootDir)) {
      val key = watcher.poll(pollingTimeout, TimeUnit.SECONDS)
      if(key != null) {
        key.pollEvents.collect {
          case ev:WatchEvent[_] if(ev.kind != OVERFLOW) => (ev.context.asInstanceOf[Path], ev.kind)
        }.foreach { case (path, kind) =>
            val absolutePath = rootDir.resolve(path).toAbsolutePath()
            println(absolutePath)
            println(Paths.get(".").toAbsolutePath())
            if(kind == ENTRY_CREATE) {
              println(Files.isDirectory(absolutePath))
              if(Files.isDirectory(absolutePath)) {
                //start observer for new directory
                println(s"directory $path")
                observer ! NewDir(absolutePath)
              } else
                observer ! NewFile(absolutePath)
            } else if(kind == ENTRY_DELETE) {
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
