package de.thm.mope.lsp.messages

import de.thm.mope.lsp._
import de.thm.mope.position.FileWithLine

case class Location(uri:DocumentUri, range:Range)

object Location {
  def apply(file:FileWithLine):Location = {
    val pos = Position(file.line, 0)
    Location(new java.net.URI(file.path), Range(pos,pos))
  }
}