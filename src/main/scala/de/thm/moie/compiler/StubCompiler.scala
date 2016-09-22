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

package de.thm.moie.compiler
import de.thm.moie.doc.DocInfo
import de.thm.moie.suggestion.Suggestion._
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
  override def getClasses(className:String): Set[(String, Kind.Value)] = Set[(String, Kind.Value)]()
  override def getParameters(className:String): List[(String, Option[String])] = List[(String, Option[String])]()
  override def getClassDocumentation(className:String): Option[String] = None
  override def getGlobalScope(): Set[(String, Kind.Value)] = Set[(String, Kind.Value)]()
}
