package de.thm.moie.compiler

import java.nio.file.Path

import scala.concurrent.{ExecutionContext, Future}

trait AsyncModelicaCompiler {
  this: ModelicaCompiler =>
  def compileAsync(files:List[Path])(
    implicit context:ExecutionContext): Future[Seq[CompilerError]] =
      Future(compile(files))
}
