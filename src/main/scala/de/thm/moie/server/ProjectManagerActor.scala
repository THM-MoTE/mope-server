/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection._

import akka.pattern.{pipe, ask}
import akka.actor.{Actor, Props}
import akka.util.Timeout
import de.thm.moie.compiler.{ModelicaCompiler, CompilerError}
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

  implicit val timeout = Timeout(5 seconds)

  //initialize all files
  val rootDir = Paths.get(description.path)
  val fileWatchingActor = context.actorOf(Props(new FileWatchingActor(rootDir, description.outputDirectory)))

  override def handleMsg: Receive = {
    case CompileProject =>
      (for {
        files <- (fileWatchingActor ? FileWatchingActor.GetFiles).mapTo[List[Path]]
        errors <- compiler.compileAsync(files)
        _ = printDebug(errors)
      } yield errors) pipeTo sender
  }

  private def printDebug(errors:Seq[CompilerError]): Unit = {
    log.debug(s"Compiled project ${description.path} with" +
      (if(errors.isEmpty) " no errors" else errors.mkString("\n"))
    )
  }

  override def postStop(): Unit = {
    log.info("stopping")
  }
}

object ProjectManagerActor {
  sealed trait ProjectManagerMsg
  case object CompileProject extends ProjectManagerMsg
}
