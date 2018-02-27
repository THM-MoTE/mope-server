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

package de.thm

import java.nio.file.Path

import akka.actor.{ActorRef, Props}
import com.softwaremill.tagging._
import de.thm.mope.declaration.JumpToLike
import de.thm.mope.doc.DocumentationLike
import de.thm.mope.project.ProjectDescription
import de.thm.mope.suggestion.{CompletionLike, PrefixMatcher, SrcFileInspector}

package object mope {
  type Filter[A] = A => Boolean
  type PathFilter = Filter[Path]

  import tags._

  //factories
  type Factory[A] = () => A
  type SrcFileFactory = Path => SrcFileInspector
  type PrefixMatcherFactory = String => PrefixMatcher
  type SuggestionProviderPropsFactory = CompletionLike => Props @@ CompletionMarker
  type JumpToPropsFactory = JumpToLike => Props @@ JumpProviderMarker
  type DocumentationProviderPropsFactory = DocumentationLike => Props @@ DocProviderMarker
  type ProjectsManagerRef = ActorRef @@ ProjectsManagerMarker
  type ProjectManagerPropsFactory = (ProjectDescription, Int) => Props
  //@@ProjectManagerMarker
  type RecentHandlerProps = Props @@ RecentHandlerMarker

  /** Tags for the injector to identify actors. */
  object tags {

    sealed trait RecentHandlerMarker

    sealed trait ProjectsManagerMarker

    sealed trait ProjectManagerMarker

    sealed trait JumpProviderMarker

    sealed trait DocProviderMarker

    sealed trait FileWatchingMarker

    sealed trait CompletionMarker

    sealed trait RecentFileMarker

    sealed trait DocMarker

    sealed trait MissingDocMarker

  }

}
