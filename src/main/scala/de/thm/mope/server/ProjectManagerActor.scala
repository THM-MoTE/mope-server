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
import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import de.thm.mope.tree._
import de.thm.mope.compiler.{CompilerError, ModelicaCompiler}
import de.thm.mope.declaration.{DeclarationRequest, JumpToProvider}
import de.thm.mope.doc.DocumentationProvider
import de.thm.mope.doc.DocumentationProvider.{GetClassComment, GetDocumentation}
import de.thm.mope.project.{InternalProjectConfig, ProjectDescription}
import de.thm.mope.server.FileWatchingActor.{DeletedPath, GetFiles, NewPath}
import de.thm.mope.suggestion.{CompletionRequest, SuggestionProvider, TypeRequest}
import de.thm.mope.utils.ThreadUtils
import de.thm.mope.utils.actors.UnhandledReceiver

import scala.collection._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/** A manager for one specific project described by the given `description`.
  * This actor is the main-entry-point for a connected project.
 *
  * @note This actor starts several subactors for serving all requests
  */
class ProjectManagerActor(description:ProjectDescription,
                          compiler:ModelicaCompiler,
                          indexFiles:Boolean = true)
  extends Actor
  with UnhandledReceiver
  with ActorLogging {

  import ProjectManagerActor._
  import context.dispatcher

  implicit val timeout = Timeout(5 seconds)

  val executor = Executors.newCachedThreadPool(ThreadUtils.namedThreadFactory("MOPE-"+self.path.name))
  implicit val projConfig = InternalProjectConfig(executor, timeout)
  val rootDir = Paths.get(description.path)
  val fileWatchingActor = context.actorOf(Props(new FileWatchingActor(self, rootDir, description.outputDirectory)))
  val completionActor = context.actorOf(Props(new SuggestionProvider(compiler)))
  val jumpProvider = context.actorOf(Props(new JumpToProvider(compiler)))
  val docProvider = context.actorOf(Props(new DocumentationProvider(compiler)))

  private var projectFiles:TreeLike[Path] = FileSystemTree(rootDir, {p:Path => Files.isDirectory(p) || FileWatchingActor.moFileFilter(p)})

  // def getProjectFiles:Future[List[Path]] =
  //   if(indexFiles) Future.successful(projectFiles)
  //   else Future(FileWatchingActor.getFiles(rootDir, FileWatchingActor.moFileFilter).sorted)

  def getProjectFiles:Future[TreeLike[Path]] =
    Future.successful(projectFiles)

  private def getDefaultScriptPath:Future[Path] = Future {
    val defaultScript = description.buildScript.getOrElse("build.mos")
    val path = rootDir.resolve(defaultScript)
    if(Files.exists(path) && Files.isRegularFile(path))
      path
    else
      throw new NotFoundException(s"Can't find script called $defaultScript!")
  }

  def withExists[T](p:Path)(fn: => Future[T]): Future[T] =
    if(Files.exists(p)) fn
    else Future.failed(new NotFoundException(s"Can't find file $p!"))

  override def preStart() = ()

 def errorInProjectFile(error:CompilerError): Boolean = {
    val path = Paths.get(error.file)
     error.file.isEmpty ||
     path.startsWith(rootDir) ||
     path.startsWith(rootDir.toRealPath()) ||
     error.file.endsWith(".mos")
   }

  // override def receive: Receive = {
  //   case InitialInfos(files) =>
  //     projectFiles = files.toList.sorted
  //     context become initialized
  // }
  override def receive: Receive = initialized

  private def initialized: Receive =
    forward.orElse(compile)/*.
    orElse(updateFileIndex).
    orElse({
    case CheckModel(file) =>
      withExists(file)(for {
        files <- getProjectFiles
        msg <- compiler.checkModelAsync(files, file)
        _ = log.debug("Checked model from {} with {}", file, msg:Any)
      } yield msg) pipeTo sender
    })*/

  private def forward: Receive = {
    case x:CompletionRequest => completionActor forward x
    case x:TypeRequest => completionActor forward x
    case x:DeclarationRequest => jumpProvider forward x
    case x:GetDocumentation => docProvider forward x
  }

  private def compile: Receive = {
    case CompileProject(file) =>
      withExists(file) {
        for {
          tree <- getProjectFiles
          _ = log.debug("compiling with project: {}", tree.label)
          errors <- Future(compiler.compile(tree, file))
          filteredErrors = errors.filter(errorInProjectFile)
          _ = printDebug(filteredErrors)
        } yield filteredErrors
      } pipeTo sender
    case CompileScript(path) =>
      withExists(path) {
        for {
          errors <- compiler.compileScriptAsync(path)
          filteredErrors = errors.filter(errorInProjectFile)
          _ = printDebug(filteredErrors)
        } yield filteredErrors
      } pipeTo sender
    case CompileDefaultScript =>
      (for {
        path <- getDefaultScriptPath
        errors <- compiler.compileScriptAsync(path)
        filteredErrors = errors.filter(errorInProjectFile)
        _ = printDebug(filteredErrors)
      } yield filteredErrors) pipeTo sender
  }

/*
  private def updateFileIndex: Receive = {
    case NewFiles(files) =>
      projectFiles = (files ++ projectFiles).toList.sorted
    case NewPath(p) if Files.isDirectory(p) =>
      (fileWatchingActor ? GetFiles(p)).
        mapTo[List[Path]].
        foreach{ files => self ! NewFiles(files) }
    case NewPath(p) =>
      projectFiles = (p :: projectFiles).sorted
    case DeletedPath(p) =>
      projectFiles = projectFiles.filterNot { path => path.startsWith(p) }
  }*/

  private def printDebug(errors:Seq[CompilerError]): Unit = {
    log.debug("Compiled project {} with {}", description.path,
      if(errors.isEmpty) " no errors" else errors.mkString("\n"))
  }

  override def postStop(): Unit = {
    compiler.stop()
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
  case class CompileProject(file:Path) extends ProjectManagerMsg
  case object CompileDefaultScript extends ProjectManagerMsg
  case class CompileScript(path:Path) extends ProjectManagerMsg
  case class CheckModel(path:Path) extends ProjectManagerMsg
  private[ProjectManagerActor] case class InitialInfos(files:Seq[Path])
  private[ProjectManagerActor] case class UpdatedCompilerErrors(errors:Seq[CompilerError])
  private[ProjectManagerActor] case class NewFiles(files:Seq[Path])
}
