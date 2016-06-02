package de.thm.moie.compiler

import java.nio.file.Path

trait ModelicaCompiler {
  def compile(files:List[Path]): Seq[CompilerError]
}
