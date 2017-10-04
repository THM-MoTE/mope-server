package de.thm.mope.lsp.messages

import de.thm.mope.position.FilePosition

case class Position(line:Int, character: Int) {
  def filePosition:FilePosition = FilePosition(line+1,character+1)
}

object Position {
  def apply(fp:FilePosition):Position = Position(fp.line-1, fp.column-1)
}