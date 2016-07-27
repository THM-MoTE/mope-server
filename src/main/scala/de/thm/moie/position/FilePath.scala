/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.position

import java.nio.file.{Path, Paths}

import scala.language.implicitConversions

/** A path to a file */
case class FilePath(path:String)

object FilePath {
  implicit def filePathToNioPath(fp:FilePath):Path = Paths.get(fp.path)
}
