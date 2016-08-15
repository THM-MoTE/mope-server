/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler
import de.thm.moie.doc.DocInfo
import de.thm.moie.suggestion.CompletionResponse._
import de.thm.moie.position.FilePosition
import java.nio.file.Path

trait StubCompiler
  extends ModelicaCompiler {

  override def compileScript(path:Path): Seq[CompilerError] =
    Seq(CompilerError("Warning",
      "",
      FilePosition(0, 0),
      FilePosition(0, 0),
      "Compiling a script isn't supported by this compiler"))
  override def checkModel(files:List[Path], path:Path): String =
    "Warning: it's not possible to check a model with this compiler"
  override def getSrcFile(className:String): Option[String] = None
  override def getDocumentation(className:String): Option[DocInfo] = None
  override def getClasses(className:String): Set[(String, CompletionType.Value)] = Set[(String, CompletionType.Value)]()
  override def getParameters(className:String): List[(String, Option[String])] = List[(String, Option[String])]()
  override def getClassDocumentation(className:String): Option[String] = None
  override def getGlobalScope(): Set[(String, CompletionType.Value)] = Set[(String, CompletionType.Value)]()
}
