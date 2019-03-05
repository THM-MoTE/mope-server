package de.thm.mope

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import de.thm.mope.compiler.CompilerError
import de.thm.mope.position.FilePosition

object TestHelpers {

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

  def createFile(resourceURI:String, filePath:Path): Path = {
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
}
