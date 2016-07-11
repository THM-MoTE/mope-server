/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import de.thm.moie.compiler.{CompilerError, FilePosition}

package object moie {

  def invalidFileError(file:Path) = CompilerError("Error",
    file.toRealPath().toString,
    FilePosition(1,6),
    FilePosition(3,9),
    "Parse error: The identifier at start and end are different")

  def invalidScriptError(file:Path) = CompilerError("Error",
    file.toRealPath().toString,
    FilePosition(1,1),
    FilePosition(1,23),
    "Klasse lodFile konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden.")

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
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      override def postVisitDirectory(dir:Path, exc:java.io.IOException):FileVisitResult = {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
