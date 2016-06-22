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
  def word = """[a-zA-Z0-9_\.,\+\-\*\/:\(\)'\"\}\{\[\]\;=<>]+""".r
  def character = """[a-zA-Z]""".r

  def pathDelimiter:Parser[String] = ("/" | "\\")

  def msgParser: Parser[List[CompilerError]] =
    skipUninterestingStuff ~> (errorLine +)

  def skipUninterestingStuff =
    ((not("\\[|\\{".r) ~> ident ~> """([^\n]+)""".r) *)

      //TODO pack common things of | together in one production
  def errorLine: Parser[CompilerError] = (
    ("["~> path <~ ":") ~ (filePosition <~ "-") ~ (filePosition <~ ":" <~ ident <~ "]") ~
    (("Error" | "Warning") <~ ":") ~
    errorMsg("\\[") ^^ {
      case path ~ start ~ end ~ tpe ~ msg =>
        CompilerError(tpe, path, start, end, msg)
    }
    | ("{" ~> "\"" ~> "[" ~> path <~ ":") ~ (filePosition <~ "-") ~ (filePosition <~ ":" <~ ident <~ "]") ~
    (("Error" | "Warning") <~ ":") ~
    errorMsg("\\[|\\{") ^^ {
      case path ~ start ~ end ~ tpe ~ msg =>
      val delimiter = "\""
      if(msg.contains(delimiter))
        CompilerError(tpe, path, start, end, msg.substring(0, msg.indexOf(delimiter)))
      else CompilerError(tpe, path, start, end, msg)
    })

  def errorMsg(additionalDelimiter:String): Parser[String] =
    (errorSub(additionalDelimiter) +) ^^ { _.mkString("\n") }

  def errorSub(additionalDelimiter:String): Parser[String] =
    rep1sep((not("Error" | "Warning" | additionalDelimiter.r) ~> word), "") ^^ {
      case words =>
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
