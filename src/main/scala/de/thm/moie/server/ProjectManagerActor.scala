/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import java.util.concurrent.Executors
import scala.collection._

import akka.pattern.pipe
import akka.actor.{Actor, Props}
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

  //initialize all files
  val rootDir = Paths.get(description.path)
  val fileWatchingActor = context.actorOf(Props(new FileWatchingActor(rootDir, description.outputDirectory)))

  override def handleMsg: Receive = {
    case CompileProject =>
      //compiler.compileAsync(files.toList) pipeTo sender
  }

  override def postStop(): Unit = {
    log.info("stopping")
  }
}

object ProjectManagerActor {
  sealed trait ProjectManagerMsg
  case object CompileProject extends ProjectManagerMsg
}
