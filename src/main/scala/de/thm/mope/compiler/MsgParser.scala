/**
  * Copyright (C) 2016,2017 Nicola Justus <nicola.justus@mni.thm.de>
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

/** A (simple) parser for OMC's error messages */
class MsgParser extends RegexParsers with CommonParsers with ImplicitConversions {
  def word = """[\w\.\+\-\*_,;:=<>?!\(\)\{\}\/\\"'äöü]+""".r

  def hyphenedError: Parser[String] =
    """\"Error""".r | "Error"

  def msgParser: Parser[List[CompilerError]] = (
    skipUnused ~> ("\"" ?) ~> (error +)
      | processFile ~ (skipNotifications ~> "Error" ~> ":" ~> errorMsg) ^^ {
      case path ~ msg =>
        List(CompilerError("Error", path, unknownPosition, unknownPosition, msg))
    }
      | ((skipHyphenedError ~> hyphenedError ~> ":" ~> errorMsg ^^ {
      case msg => CompilerError("Error", unknownPath, unknownPosition, unknownPosition, killDanglingHyphens(msg))
    }) +)
    )

  def skipUnused = ((not("[") ~> word) *)

  def skipHyphenedError = ((not(hyphenedError) ~> word) *)

  def skipNotifications = ((not("Error") ~> word) *)

  def processFile: Parser[String] = "Error" ~> "processing" ~> "file:" ~> path

  def error: Parser[CompilerError] = (
    errorPosition ~ (ident <~ ":") ~ errorMsg ^^ {
      case (path, start, end) ~ tpe ~ msg =>
        CompilerError(tpe, path, start, end, killDanglingHyphens(msg))
    }
    )

  def errorMsg: Parser[String] =
    rep1sep(not("[" | "Error") ~> word, "") ^^ {
      case xs =>
        xs.reduceLeft[String] {
          case (acc, elem) if elem == "-" => acc + "\n" + elem
          case (acc, elem) => acc + " " + elem
        }.trim()
    }

  def errorPosition: Parser[(String, FilePosition, FilePosition)] =
    ("[" ~> path <~ ":") ~ (filePosition <~ "-") ~ filePosition <~ ":" <~ ident <~ "]" ^^ {
      case path ~ start ~ end => (path, start, end)
    }

  def filePosition: Parser[FilePosition] =
    (number ~ (":" ~> number)) ^^ {
      case n1 ~ n2 => FilePosition(n1.toInt, n2.toInt)
    }

  private def killDanglingHyphens(s: String): String =
    if (s.endsWith("""\""""))
      killDanglingHyphens(s.substring(0, s.length - 2))
    else if (s.endsWith(" ") || s.endsWith("\""))
      killDanglingHyphens(s.substring(0, s.length - 1))
    else s

  def parse(msg: String): scala.util.Try[List[CompilerError]] = {
    if (msg.contains("Error")) {
      parse(msgParser, msg) match {
        case Success(result, _) => scala.util.Success(result)
        case NoSuccess(msg, input) =>
          scala.util.Failure(
            new IllegalArgumentException(
              s"Can't parse msg in line ${input.pos.line}, column ${input.pos.column}: " + msg))
      }
    } else scala.util.Success(Nil)
  }
}
