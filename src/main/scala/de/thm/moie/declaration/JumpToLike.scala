/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.declaration

trait JumpToLike {
  /** Returns the path to the source of `className` if the source exists. */
  def getSrcFile(className:String): Option[String]
}
