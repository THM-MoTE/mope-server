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

package de.thm.mope.suggestion

import java.nio.file.Path
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString


class SrcFileInspector(srcFile:Path)(implicit mat: ActorMaterializer) {
  import SrcFileInspector._

  val lines = FileIO.fromPath(srcFile).
    via(Framing.delimiter(ByteString("\n"), 8192, true)).
    map(_.utf8String)

  def typeOf(word:String, lineNo:Int): Source[TypeOf, _] = {
    val toTypeOf =
      Flow[LocalVariable].map {
        case LocalVariable(tpe,name,comment) => TypeOf(name, tpe, comment)
      }

    identRegex.r.
      findFirstIn(word).
      map { ident =>
        lines.
          take(lineNo).
          via(onlyVariables).
          via(nameEquals(ident)).
          via(toTypeOf)
      }.getOrElse(Source.empty)
  }

  private def nameEquals(word:String) =
    Flow[LocalVariable].filter { x =>
      x.name == word
    }

  def localVariables(lineNo:Option[Int]): Source[LocalVariable, _] = {
    val srcLines = lineNo match {
      case Some(no) => lines.take(no)
      case None => lines
    }
    srcLines.via(onlyVariables)
  }

  def onlyVariables =
    Flow[String].collect {
      case variableCommentRegex(tpe,name,comment) => LocalVariable(tpe, name, Some(comment))
      case variableRegex(tpe, name) => LocalVariable(tpe,name,None)
    }
}

object SrcFileInspector {
  val ignoredModifiers =
    "(?:" + List("(?:parameter)",
                 "(?:discrete)",
                 "(?:input)",
                 "(?:output)",
                 "(?:flow)").mkString("|") + ")"
  val typeRegex = """(\w[\w\-\_\.]*)"""
  val identRegex = """(\w[\w\-\_]*)"""
  val commentRegex = """"([^"]+)";"""

  val variableRegex =
    s"""\\s*(?:$ignoredModifiers\\s+)?$typeRegex\\s+$identRegex.*""".r
  val variableCommentRegex =
    s"""\\s*(?:$ignoredModifiers\\s+)?$typeRegex\\s+$identRegex.*\\s+$commentRegex""".r

  def nonEmptyLines(line:String):Boolean = !line.isEmpty

  case class LocalVariable(`type`:String, name:String, docString:Option[String])
}
