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

package de.thm.mope

import akka.actor.{ActorSystem, ActorRef, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout

import java.nio.file.{Path,Paths}
import java.nio.charset.Charset
import java.util.concurrent.{Executors, ExecutorService, TimeUnit}

import com.softwaremill.macwire._
import com.softwaremill.macwire.akkasupport._
import com.softwaremill.tagging._
import com.typesafe.config.Config
import scala.concurrent.duration._
import scala.language.postfixOps

import de.thm.mope.compiler._
import de.thm.mope.project._
import de.thm.mope.declaration._
import de.thm.mope.doc._
import de.thm.mope.server._
import de.thm.mope.suggestion._
import de.thm.mope.utils.ThreadUtils
import de.thm.mope.config.ServerConfig

import de.thm.mope.tags._
trait MopeModule {
  lazy val serverConfig:ServerConfig = ServerConfig(
    config,
    Executors.newCachedThreadPool(ThreadUtils.namedThreadFactory("MOPE-IO")))(
    Timeout(config.getInt("defaultAskTimeout") seconds),
    actorSystem.dispatchers.lookup("akka.dispatchers.blocking-io"))

  import serverConfig._

  lazy val ensembleHandler:EnsembleHandler = wire[EnsembleHandler]
  lazy val compilerFactory:CompilerFactory = wire[CompilerFactory]
  def projRegister:ProjectRegister = wire[ProjectRegister]
  lazy val recentFilesHandler:ActorRef@@RecentHandlerMarker = wireActor[RecentFilesActor]("recent-files").taggedWith[RecentHandlerMarker]
  lazy val projectsManager:ActorRef@@ProjectsManagerMarker = wireActor[ProjectsManagerActor]("projects-manager").taggedWith[ProjectsManagerMarker]

  lazy val inspectorFactory: Path => SrcFileInspector = p => wire[SrcFileInspector]
  lazy val prefixFactory: String => PrefixMatcher = (s:String) => wire[PrefixMatcher]
  lazy val jumpToProviderFactory:JumpToLike => ActorRef@@JumpProviderMarker = (j:JumpToLike) => actorSystem.actorOf(Props(wire[JumpToProvider])).taggedWith[JumpProviderMarker]
  lazy val docProviderFactory:DocumentationLike => ActorRef@@DocProviderMarker = (d:DocumentationLike) => wireAnonymousActor[DocumentationProvider].taggedWith[DocProviderMarker]
  lazy val fileWatchingActorFactory:(ActorRef,Path,String) => ActorRef@@FileWatchingMarker = (a:ActorRef, r:Path, o:String) => wireAnonymousActor[FileWatchingActor].taggedWith[FileWatchingMarker]
  lazy val suggestionProviderFactory:CompletionLike => ActorRef@@CompletionMarker = (c:CompletionLike) => actorSystem.actorOf(Props(wire[SuggestionProvider])).taggedWith[CompletionMarker]

  lazy val projManagerFactory:(ProjectDescription,Int) => ActorRef@@ProjectManagerMarker = (d:ProjectDescription, id:Int) => {
    val compiler = compilerFactory.newCompiler(Paths.get(d.path).resolve(d.outputDirectory))
    val doc:ActorRef@@DocProviderMarker = docProviderFactory(compiler)
    val jump:ActorRef@@JumpProviderMarker = jumpToProviderFactory(compiler)
    val sug:ActorRef@@CompletionMarker = suggestionProviderFactory(compiler)
    wireActor[ProjectManagerActor]("project-manager-$id").taggedWith[ProjectManagerMarker]
  }

  lazy val router = wire[Routes]
  def config:Config
  implicit def actorSystem:ActorSystem
  implicit def mat:ActorMaterializer
}
