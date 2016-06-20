/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file._

import akka.actor.{Actor, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import de.thm.moie.compiler.{CompilerError, ModelicaCompiler}
import de.thm.moie.project.ProjectDescription
import de.thm.moie.server.FileWatchingActor.{DeletedPath, GetFiles, ModifiedPath, NewPath}
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.collection._
import scala.concurrent.duration._
import scala.language.postfixOps

class ProjectManagerActor(description:ProjectDescription,
                          compiler:ModelicaCompiler)
  extends Actor
  with UnhandledReceiver
  with LogMessages {

  import ProjectManagerActor._
  import context.dispatcher

  implicit val timeout = Timeout(5 seconds)

  val rootDir = Paths.get(description.path)
  val fileWatchingActor = context.actorOf(Props(new FileWatchingActor(self, rootDir, description.outputDirectory)))

  private val projectFiles = mutable.ArrayBuffer[Path]()
  private val compileErrors = mutable.ArrayBuffer[CompilerError]()

  override def preStart() =
    for {
      files <- (fileWatchingActor ? GetFiles).mapTo[List[Path]]
      errors <- compiler.compileAsync(files).map(_.filter(errorInProjectFile))
    } {
      self ! InitialInfos(files, errors)
    }

  def errorInProjectFile(error:CompilerError): Boolean =
    Paths.get(error.file).startsWith(rootDir)

  override def handleMsg: Receive = {
    case InitialInfos(files, errors) =>
      projectFiles.clear()
      compileErrors.clear()
      projectFiles ++= files
      compileErrors ++= errors
      log.debug("new init infos: files {} errors {}", projectFiles, compileErrors)
      context become initialized
  }

  private def initialized: Receive = {
    case CompileProject =>

      compileErrors.toSeq
    case ModifiedPath(p) => log.debug("path modified {}", p)
    case NewPath(p) => log.debug("path new {}", p)
    case DeletedPath(p) => log.debug("path delete {}", p)
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
  private[ProjectManagerActor] case class InitialInfos(files:Seq[Path], errors:Seq[CompilerError])

  //all hold infos about a modelica file
  case class ModelicaInfo(errors:Seq[CompilerError])
}
