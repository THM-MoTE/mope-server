/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

trait JumpToLike {
  def getSrcFile(className:String): Option[String]
}
