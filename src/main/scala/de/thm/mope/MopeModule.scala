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

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import java.nio.file.{Path, Paths}
import java.nio.charset.Charset
import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

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
import de.thm.mope.config.{ProjectConfig, ServerConfig}
import de.thm.mope.tags._
trait MopeModule {
  lazy val serverConfig:ServerConfig = {
    val executor = Executors.newCachedThreadPool(ThreadUtils.namedThreadFactory("MOPE-IO"))
    actorSystem.registerOnTermination {
      executor.shutdown()
    }
    ServerConfig(
      config,
      executor)(
      Timeout(config.getInt("defaultAskTimeout") seconds),
      actorSystem.dispatchers.lookup("akka.dispatchers.blocking-io"))
  }

  import serverConfig.{timeout, blockingDispatcher, recentFiles}

  lazy val ensembleHandler:EnsembleHandler = wire[EnsembleHandler]
  lazy val compilerFactory:CompilerFactory = wire[CompilerFactory]
  def projRegister:ProjectRegister = wire[ProjectRegister]
  lazy val recentFilesProps:Props@@RecentHandlerMarker = wireProps[RecentFilesActor].taggedWith[RecentHandlerMarker]
  lazy val projectsManager:ActorRef@@ProjectsManagerMarker = wireActor[ProjectsManagerActor]("projects-manager").taggedWith[ProjectsManagerMarker]

  lazy val inspectorFactory: SrcFileFactory = p => wire[SrcFileInspector]
  lazy val prefixFactory: PrefixMatcherFactory = (s:String) => wire[PrefixMatcher]
  lazy val jumpToProps:JumpToPropsFactory = (j:JumpToLike) => wireProps[JumpToProvider].taggedWith[JumpProviderMarker]
  lazy val docProviderFactory:DocumentationProviderPropsFactory = (d:DocumentationLike) => wireProps[DocumentationProvider].taggedWith[DocProviderMarker]
  lazy val suggestionProviderFactory:SuggestionProviderPropsFactory = (c:CompletionLike) => wireProps[SuggestionProvider].taggedWith[CompletionMarker]

  lazy val projManagerFactory:ProjectManagerPropsFactory = (d:ProjectDescription, id:Int) => {
    val compiler:ModelicaCompiler = compilerFactory.newCompiler(d)
    val conf:ProjectConfig = ProjectConfig(serverConfig, d)
    wireProps[ProjectManagerActor]
  }

  lazy val router = wire[Routes]
  lazy val server = wire[Server]
  def config:Config
  implicit def actorSystem:ActorSystem
  implicit def mat:ActorMaterializer
}
