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

import de.thm.mope.position.FilePosition

import scala.language.postfixOps
import scala.util.parsing.combinator.{ImplicitConversions, RegexParsers}

/** A helper trait containing common-parsers for parsing compiler-errors.
  */
trait CommonParsers extends RegexParsers with ImplicitConversions {
  def fileSeparator = if(omc.Global.isWindowsOS) "\\" else "/"
  // regex from: http://stackoverflow.com/a/5954831
  override val whiteSpace = """(\s|//.*|(?m)/\*(\*(?!/)|[^*])*\*/)+""".r

  def unknownPosition:FilePosition = FilePosition(0,0)
  def unknownError:String = "Compiler didn't provide any further message"
  def unknownPath:String = ""

  def pathDelimiter = """(?:/|\\){1,2}""".r
  def pathIdent = """[a-z-A-Z-.+<>0-9?=_ ]+""".r
  def number = """[0-9]+""".r
  def ident =  """[a-zA-Z0-9]+""".r
  def character = "[a-zA-Z]".r

  /** Parses a path similar to: /home/user/awesome/project.txt */
  def path:Parser[String] =
    (root ?) ~ rep1sep(pathIdent, pathDelimiter) ^^ {
      case None ~ lst => lst.mkString(fileSeparator)
      case Some(root) ~ lst => root + lst.mkString(fileSeparator)
    }

  /** Parses the root-node of a filesystem. In c:\ in Windows, / in Linux */
  def root: Parser[String] =
    ("/" //unix style
    | character ~ ":" ~ pathDelimiter ^^ { case ch ~ _ ~ _ => ch + ":" + fileSeparator } //windows style
    )
}
