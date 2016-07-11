/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import scala.language.postfixOps
import scala.util.parsing.combinator.{ImplicitConversions, RegexParsers}

class MsgParser extends RegexParsers with CommonParsers with ImplicitConversions {
  def word = """[\w\.\+\-\*_,;:=<>?!\(\)\{\}\/\\"'äöü]+""".r

  def hyphenedError:Parser[String] =
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

  def processFile:Parser[String] = "Error" ~> "processing" ~> "file:" ~> path

  def error:Parser[CompilerError] = (
    errorPosition ~ (ident <~ ":") ~ errorMsg ^^ {
      case (path, start, end) ~ tpe ~ msg =>
        CompilerError(tpe, path, start, end, killDanglingHyphens(msg))
    }
  )

  def errorMsg: Parser[String] =
    rep1sep(not("[" | "Error") ~> word, "") ^^ {
      case xs =>
        xs.reduceLeft[String] {
          case (acc,elem) if elem == "-" => acc + "\n" + elem
          case (acc,elem) => acc + " " + elem
        }.trim()
    }

  def errorPosition: Parser[(String, FilePosition, FilePosition)] =
    ("["~> path <~ ":") ~ (filePosition <~ "-") ~ filePosition <~ ":" <~ ident <~ "]" ^^ {
      case path ~ start ~ end => (path,start,end)
    }

  def filePosition: Parser[FilePosition] =
    (number ~ (":" ~> number)) ^^ {
      case n1 ~ n2 => FilePosition(n1.toInt,n2.toInt)
    }

  private def killDanglingHyphens(s:String):String =
    if(s.endsWith("""\""""))
      killDanglingHyphens(s.substring(0, s.length-2))
    else if(s.endsWith(" ") || s.endsWith("\""))
      killDanglingHyphens(s.substring(0, s.length-1))
    else s

  def parse(msg:String): scala.util.Try[List[CompilerError]] = {
    if(msg.contains("Error")) {
      parse(msgParser, msg) match {
        case Success(result, _)    => scala.util.Success(result)
        case NoSuccess(msg, input) =>
          scala.util.Failure(
            new IllegalArgumentException(
              s"Can't parse msg in line ${input.pos.line}, column ${input.pos.column}: "+msg))
      }
    } else scala.util.Success(Nil)
  }
}
