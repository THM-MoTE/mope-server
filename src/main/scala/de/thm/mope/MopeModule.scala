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

import akka.actor.{ActorSystem, ActorRef}
import akka.stream.ActorMaterializer

import java.nio.file.{Path,Paths}
import java.util.concurrent.{Executors, TimeUnit}

import com.softwaremill.macwire._
import com.softwaremill.macwire.akkasupport._
import com.softwaremill.tagging._

import de.thm.mope.compiler._
import de.thm.mope.project._
import de.thm.mope.declaration._
import de.thm.mope.doc._
import de.thm.mope.server._
import de.thm.mope.suggestion._
import de.thm.mope.utils.ThreadUtils

trait MopeModule {
  import tags._
  lazy val config = Global.config //FIXME: inject & kill the global object
  lazy val encoding = Global.encoding

  //TODO: use 1 executor for all projects? move up into ProjectSmanager?
  lazy val executor = Executors.newCachedThreadPool(ThreadUtils.namedThreadFactory("MOPE-IO"))


  lazy val compilerFactory:CompilerFactory = wire[CompilerFactory]
  lazy val projRegister:ProjectRegister = wire[ProjectRegister]
  lazy val recentFilesHandler:ActorRef@@RecentHandlerMarker = wireActor[RecentFilesActor]("recent-files").taggedWith[RecentHandlerMarker]
  lazy val projectsManager:ActorRef@@ProjectsManagerMarker = wireActor[ProjectsManagerActor]("projects-manager").taggedWith[ProjectsManagerMarker]

  lazy val inspectorFactory: Path => SrcFileInspector = p => wire[SrcFileInspector]
  lazy val prefixFactory: String => PrefixMatcher = (s:String) => wire[PrefixMatcher]
  lazy val jumpToProviderFactory:JumpToLike => ActorRef@@JumpProviderMarker = (j:JumpToLike) => wireAnonymousActor[JumpToProvider].taggedWith[JumpProviderMarker]
  lazy val docProviderFactory:DocumentationLike => ActorRef@@DocProviderMarker = (d:DocumentationLike) => wireAnonymousActor[DocumentationProvider].taggedWith[DocProviderMarker]
  lazy val fileWatchingActorFactory:(ActorRef,Path,String) => ActorRef@@FileWatchingMarker = (a:ActorRef, r:Path, o:String) => wireAnonymousActor[FileWatchingActor].taggedWith[FileWatchingMarker]
  lazy val suggestionProviderFactory:CompletionLike => ActorRef@@CompletionMarker = (c:CompletionLike) => wireAnonymousActor[SuggestionProvider].taggedWith[CompletionMarker]

  lazy val projManagerFactory:(ProjectDescription,Int) => ActorRef@@ProjectManagerMarker = (d:ProjectDescription, id:Int) => {
    val compiler = compilerFactory.newCompiler(Paths.get(d.outputDirectory))
    val doc:ActorRef@@DocProviderMarker = docProviderFactory(compiler)
    val jump:ActorRef@@JumpProviderMarker = jumpToProviderFactory(compiler)
    val sug:ActorRef@@CompletionMarker = suggestionProviderFactory(compiler)
    wireActor[ProjectManagerActor]("project-manager-$id").taggedWith[ProjectManagerMarker]
  }

  def system:ActorSystem
  def mat:ActorMaterializer
}
