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
