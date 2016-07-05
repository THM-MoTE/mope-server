/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import java.nio.file._
import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{Actor, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import de.thm.moie.compiler.{CompilerError, ModelicaCompiler}
import de.thm.moie.project.{InternalProjectConfig, ProjectDescription}
import de.thm.moie.server.FileWatchingActor.{DeletedPath, GetFiles, ModifiedPath, NewPath}
import de.thm.moie.utils.actors.UnhandledReceiver
import de.thm.moie.utils.ThreadUtils
import scala.collection._
import scala.concurrent.Future
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

  val executor = Executors.newCachedThreadPool(ThreadUtils.namedThreadFactory("MOIE-"+self.path.name))
  implicit val projConfig = InternalProjectConfig(executor, timeout)
  val rootDir = Paths.get(description.path)
  val fileWatchingActor = context.actorOf(Props(new FileWatchingActor(self, rootDir, description.outputDirectory)))

  private var projectFiles = List[Path]()
  private var compileErrors = Seq[CompilerError]()

  def getProjectFiles = projectFiles

  private def getDefaultScriptPath:Future[Path] = Future {
    val defaultScript = description.buildScript.getOrElse("build.mos")
    val path = rootDir.resolve(defaultScript)
    if(Files.exists(path) && Files.isRegularFile(path))
      path
    else
      throw new NotFoundException(s"Can't find script called $defaultScript!")
  }

  override def preStart() =
    for {
      files <- (fileWatchingActor ? GetFiles).mapTo[List[Path]]
    } {
      self ! InitialInfos(files, Nil)
    }

 def errorInProjectFile(error:CompilerError): Boolean =
   Paths.get(error.file).startsWith(rootDir) ||
   Paths.get(error.file).startsWith(rootDir.toRealPath())

  override def handleMsg: Receive = {
    case InitialInfos(files, errors) =>
      projectFiles = files.toList.sorted
      compileErrors = errors
      context become initialized
  }

  private def initialized: Receive = {
    case CompileProject =>
      compiler.
        compileAsync(getProjectFiles).
        map(_.filter(errorInProjectFile)).
        pipeTo(sender)
    case NewFiles(files) =>
      projectFiles = (files ++ projectFiles).toList.sorted
    case NewPath(p) if Files.isDirectory(p) =>
      for {
        files <- (fileWatchingActor ? GetFiles(p)).mapTo[List[Path]]
      } yield self ! NewFiles(files)
    case NewPath(p) =>
      projectFiles = (p :: projectFiles).sorted
    case DeletedPath(p) =>
      projectFiles = projectFiles.filterNot { path => path.startsWith(p) }
    case CompileScript(path) =>
      (for {
        errors <- compiler.compileScriptAsync(path)
        filteredErrors = errors.filter(errorInProjectFile)
        _ = printDebug(filteredErrors)
      } yield filteredErrors) pipeTo sender
    case CompileDefaultScript =>
      (for {
        path <- getDefaultScriptPath
        errors <- compiler.compileScriptAsync(path)
        filteredErrors = errors.filter(errorInProjectFile)
        _ = printDebug(filteredErrors)
      } yield filteredErrors) pipeTo sender
  }

  private def printDebug(errors:Seq[CompilerError]): Unit = {
    log.debug("Compiled project {} with {}", description.path,
      if(errors.isEmpty) " no errors" else errors.mkString("\n"))
  }

  override def postStop(): Unit = {
    executor.shutdown()
    if(!executor.awaitTermination(10, TimeUnit.SECONDS)) {
      log.warning("Force shutdown threadpool")
      executor.shutdownNow()
    }

    log.info("stopping")
  }
}

object ProjectManagerActor {
  sealed trait ProjectManagerMsg
  case object CompileProject extends ProjectManagerMsg
  case object CompileDefaultScript extends ProjectManagerMsg
  case class CompileScript(path:Path) extends ProjectManagerMsg
  private[ProjectManagerActor] case class InitialInfos(files:Seq[Path], errors:Seq[CompilerError])
  private[ProjectManagerActor] case class UpdatedCompilerErrors(errors:Seq[CompilerError])
  private[ProjectManagerActor] case class NewFiles(files:Seq[Path])
}
