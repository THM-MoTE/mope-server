/**
  * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */

package de.thm.mope.compiler

import java.nio.file.Path

import de.thm.mope._
import de.thm.mope.declaration.JumpToLike
import de.thm.mope.doc.DocumentationLike
import de.thm.mope.suggestion.CompletionLike
import de.thm.mope.tree.TreeLike

import scala.concurrent.{ExecutionContext, Future}

/** Defines behaviour for Modelica compilers or compiler interfaces. */
trait ModelicaCompiler
  extends CompletionLike
    with JumpToLike
    with DocumentationLike {

  protected val isPackageMo: PathFilter = _.endsWith("package.mo")

  /** Disconnects this compiler from his backend. */
  def stop(): Unit

  /** Compiles the given `files`, typechecks the `openedFile` and returns found errors.
    * `openedFile` should be the currently opened file inside the editor.
    */
  def compile(projectTree: TreeLike[Path], openedFile: Path): Seq[CompilerError]

  /** Executes the given script and returns found errors. */
  def compileScript(path: Path): Seq[CompilerError]

  /** Future-wrapped version of compileScript() */
  def compileScriptAsync(path: Path)(
    implicit context: ExecutionContext): Future[Seq[CompilerError]] =
    Future(compileScript(path))

  /** Checks the model inside of `path` and returns a information string which contains
    * the # of variables and equations.
    * `files` are needed to compile all models.
    * This list should be identical to the `files` list in compile().
    */
  def checkModel(projectTree: TreeLike[Path], path: Path): String

  override def getClassComment(className: String): Option[String] = getClassDocumentation(className)
}
