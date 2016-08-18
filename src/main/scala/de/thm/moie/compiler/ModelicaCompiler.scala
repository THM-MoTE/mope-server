/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import java.nio.file.Path

import de.thm.moie.declaration.JumpToLike
import de.thm.moie.doc.DocumentationLike
import de.thm.moie.suggestion.CompletionLike

import scala.concurrent.{ExecutionContext, Future}

/** Defines behaviour for Modelica compilers or compiler interfaces. */
trait ModelicaCompiler
  extends CompletionLike
    with JumpToLike
    with DocumentationLike {

  protected val isPackageMo:Path => Boolean = _.endsWith("package.mo")

  /** Disconnects this compiler from his backend. */
  def stop(): Unit

  /** Compiles the given `files`, typechecks the `openedFile` and returns found errors.
    * `openedFile` should be the currently opened file inside the editor.
    */
  def compile(files:List[Path], openedFile:Path): Seq[CompilerError]

  /** Future-wrapped version of compile() */
  def compileAsync(files:List[Path], openedFile:Path)(
    implicit context:ExecutionContext): Future[Seq[CompilerError]] =
    Future(compile(files, openedFile))

  /** Executes the given script and returns found errors. */
  def compileScript(path:Path): Seq[CompilerError]

  /** Future-wrapped version of compileScript() */
  def compileScriptAsync(path:Path)(
    implicit context:ExecutionContext): Future[Seq[CompilerError]] =
    Future(compileScript(path))

  /** Checks the model inside of `path` and returns a information string which contains
    * the # of variables and equations.
    * `files` are needed to compile all models.
    * This list should be identical to the `files` list in compile().
    */
  def checkModel(files:List[Path], path:Path): String

  /** Future-wrapped version of checkModel() */
  def checkModelAsync(files:List[Path], path:Path)(
    implicit context:ExecutionContext): Future[String] =
    Future(checkModel(files, path))

  override def getClassComment(className:String): Option[String] = getClassDocumentation(className)
}
