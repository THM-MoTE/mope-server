/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import scala.language.postfixOps
import scala.util.parsing.combinator.{ImplicitConversions, RegexParsers}

class MsgParser extends RegexParsers with ImplicitConversions {
  // regex from: http://stackoverflow.com/a/5954831
  override val whiteSpace = """(\s|//.*|(?m)/\*(\*(?!/)|[^*])*\*/)+""".r

  def pathIdent = """[a-z-A-Z-.+<>0-9?=_ ]+""".r
  def number = """[0-9]+""".r
  def ident =  """[a-zA-Z0-9]+""".r
  def word = """[a-zA-Z0-9_\.,\+\-\*\/:\(\)'\"\}\{\[\]\;=]+""".r
  def character = """[a-zA-Z]""".r

  def pathDelimiter:Parser[String] = ("/" | "\\")

  def msgParser: Parser[List[CompilerError]] =
    skipUninterestingStuff ~> (errorLine +)

  def skipUninterestingStuff =
    ((not("[") ~> ident ~> """([^\n]+)""".r) *)

  def errorLine: Parser[CompilerError] =
    ("["~> path <~ ":") ~ (filePosition <~ "-") ~ (filePosition <~ ":" <~ ident <~ "]") ~
    errorMsg ^^ {
      case path ~ start ~ end ~ msg =>
        CompilerError(path, start, end, msg)
    }


  def errorMsg: Parser[String] =
    (errorSub +) ^^ { _.mkString }

  def errorSub: Parser[String] =
    "Error" ~> ":" ~> rep1sep((not("Error" | "[") ~> word), "") ^^ { words =>
      words.foldLeft("") {
        case (acc, elem) => acc + (if(elem == "-") "\n" + elem else " " + elem)
      }.trim()
    }

  def path:Parser[String] =
    (root ?) ~ rep1sep(pathIdent, pathDelimiter) ^^ {
      case None ~ lst => lst.mkString("/")
      case Some(root) ~ lst => root + lst.mkString("/")
    }

  def root: Parser[String] =
    ("/" //unix style
    | character ~ ":" ~ "\\" ^^ { case ch ~ _ ~ _ => ch +":\\" } //windows style
    )

  def filePosition: Parser[FilePosition] =
    (number ~ (":" ~> number)) ^^ {
      case n1 ~ n2 => FilePosition(n1.toInt,n2.toInt)
    }

  def parse(msg:String): scala.util.Try[List[CompilerError]] = {
    parse(msgParser, msg) match {
      case Success(result, _)    => scala.util.Success(result)
      case NoSuccess(msg, input) =>
        scala.util.Failure(
          new IllegalArgumentException(
            s"Can't parse msg in line ${input.pos.line}, column ${input.pos.column}: "+msg))
    }
  }
}
