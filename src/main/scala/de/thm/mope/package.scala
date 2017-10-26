package de.thm

import java.nio.file.Path

package object mope {
  type Filter[A] = A => Boolean
  type PathFilter = Filter[Path]

  /** Tags for the injector to identify actors. */
  object tags {
    sealed trait RecentHandlerMarker
    sealed trait ProjectsManagerMarker
    sealed trait ProjectManagerMarker
    sealed trait JumpProviderMarker
    sealed trait DocProviderMarker
    sealed trait FileWatchingMarker
    sealed trait CompletionMarker
  }
}
