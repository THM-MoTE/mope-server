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

package de.thm.mope

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import de.thm.mope.compiler.CompilerError
import de.thm.mope.position.FilePosition
import org.scalatest.{Assertion, Inspectors, Matchers}

object TestHelpers extends Matchers with Inspectors {

  def invalidFileError(file:Path) = CompilerError("Error",
    file.toRealPath().toString,
    FilePosition(1,6),
    FilePosition(3,9),
    "Parse error: The identifier at start and end are different")

  def semanticInvalidFileError(file:Path) = CompilerError("Error",
      file.toRealPath().toString,
      FilePosition(2,7),FilePosition(2,22),
      "Type mismatch in binding x = \"test\", expected subtype of Real, got type String.")

  def invalidScriptError(file:Path) = CompilerError("Error",
    file.toRealPath().toString,
    FilePosition(1,1),
    FilePosition(1,23),
    "Class lodFile not found in scope <global scope> (looking for a function or record).")

  private def createFile(resourceURI:String, filePath:Path): Path = {
    Files.copy(getClass.getResourceAsStream(resourceURI), filePath)
    filePath
  }

  def createValidFile(parent:Path):Path = {
    val path = parent.resolve("validFile.mo")
    createFile("/validFile.mo", path)
  }

  def createInvalidFile(parent:Path):Path = {
    val path = parent.resolve("invalidFile.mo")
    createFile("/invalidFile.mo", path)
  }

  def createSemanticInvalidFile(parent:Path):Path = {
    val path = parent.resolve("invalidSemanticFile.mo")
    createFile("/invalidSemanticFile.mo", path)
  }

  def createValidScript(parent:Path):Path = {
    val path = parent.resolve("validScript.mo")
    createFile("/validScript.mo", path)
  }

  def createInvalidScript(parent:Path):Path = {
    val path = parent.resolve("invalidScript.mo")
    createFile("/invalidScript.mo", path)
  }

  def removeDirectoryTree(root:Path): Unit = {
    Files.walkFileTree(root, new SimpleFileVisitor[Path]() {

      override def visitFile(file:Path, attrs:BasicFileAttributes):FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir:Path, exc:java.io.IOException):FileVisitResult = {
        Files.delete(dir)
        FileVisitResult.CONTINUE
      }
    })
  }

  def listAssert[A,B](xs:Traversable[A], ys:Traversable[B]): Assertion = {
    xs should have size (ys.size)
    xs shouldBe ys
  }
}
