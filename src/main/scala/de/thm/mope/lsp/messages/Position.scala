package de.thm.mope.lsp.messages

import de.thm.mope.position.FilePosition

case class Position(line:Int, character: Int)

object Position {
  def apply(fp:FilePosition):Position = Position(fp.line-1, fp.column-1)
}