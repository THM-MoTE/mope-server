/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import java.nio.file.Path

import scala.concurrent.{ExecutionContext, Future}

trait ModelicaCompiler {
  def compile(files:List[Path]): Seq[CompilerError]
  def compileAsync(files:List[Path])(
    implicit context:ExecutionContext): Future[Seq[CompilerError]] =
    Future(compile(files))

  def compileScript(path:Path): Seq[CompilerError]
  def compileScriptAsync(path:Path)(
    implicit context:ExecutionContext): Future[Seq[CompilerError]] =
    Future(compileScript(path))
}
