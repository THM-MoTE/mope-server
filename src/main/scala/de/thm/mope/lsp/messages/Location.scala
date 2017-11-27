package de.thm.mope.lsp.messages

import de.thm.mope.lsp._
import de.thm.mope.position.FileWithLine

case class Location(uri:DocumentUri, range:Range)

object Location {
  def apply(file:FileWithLine):Location = {
    val pos = Position(file.line-1, 0) //turn lines into indexes
    Location(file.path.toUri, Range(pos,pos))
  }
}
