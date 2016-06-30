/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import scala.language.postfixOps
import scala.util.parsing.combinator.{ImplicitConversions, RegexParsers}

class MsgParser extends RegexParsers with ImplicitConversions {
  // regex from: http://stackoverflow.com/a/5954831
  override val whiteSpace = """(\s|//.*|(?m)/\*(\*(?!/)|[^*])*\*/)+""".r

  def unknownPosition:FilePosition = FilePosition(0,0)
  def unknownError:String = "Compiler didn't provide any further message"

  def pathIdent = """[a-z-A-Z-.+<>0-9?=_ ]+""".r
  def number = """[0-9]+""".r
  def ident =  """[a-zA-Z0-9]+""".r
  def word = """[a-zA-Z0-9_\.,\+\-\*\/:\(\)'\"\}\{\[\]\;=<>äöü]+""".r
  def character = """[a-zA-Z]""".r

  private def hyphenedWord: Parser[String] =
    "\"" ~ word ~ "\"" ^^ {
      case h1 ~ w ~ h2 => h1+w+h2
    }

  def pathDelimiter:Parser[String] = ("/" | "\\")

  def msgParser: Parser[List[CompilerError]] = (
    skipUninterestingStuff ~> (errorLine +)
    | ((processingFile ~ (skipNotifications ~>
    "Error" ~> ":" ~> errorSub("\\[")) ^^ {
      case path ~ msg =>
        CompilerError("Error", path, unknownPosition, unknownPosition, msg)
    }) +)
    | ((processingFile ^^ { path =>
      CompilerError("Error", path, unknownPosition, unknownPosition, unknownError)
    }) +)
    )

  def skipUninterestingStuff =
    ((not("\\[|\\{|\"".r) ~> ident ~> """([^\n]+)""".r) *)

  def skipNotifications =
    ((not("Error") ~> ident ~> """([^\n]+)""".r) *)

  def processingFile:Parser[String] =
    "Error" ~> "processing" ~> "file" ~> ":" ~> path

  /*
  def errorLine: Parser[CompilerError] = (
    errorPosition ~ (("Error" | "Warning") <~ ":") ~ errorSub("\\[") ^^ {
      case (path, start, end) ~ tpe ~ msg =>
        CompilerError(tpe, path, start, end, msg)
    }
    | ("\"" |("{" ~> "\"")) ~> errorPosition ~ (("Error" | "Warning") <~ ":") ~ errorSub("\\[|\\{|\"") ^^ {
      case (path, start, end) ~ tpe ~ msg =>
      val delimiter = "\""
        println("parsed MESSAGE "+msg)
      if(msg.contains(delimiter))
        CompilerError(tpe, path, start, end, msg.substring(0, msg.indexOf(delimiter)))
      else CompilerError(tpe, path, start, end, msg)
    }
    )

  def errorMsg(additionalDelimiter:String): Parser[String] =
    (errorSub(additionalDelimiter) +) ^^ { _.mkString("\n") }
*/

  def errorLine: Parser[CompilerError] = (
    errorPosition ~ (("Error" | "Warning") <~ ":") ~ errorSub("\\[") ^^ {
      /** Parses something similar to:
        * [/nico/tests/ball.mo:1:6-19:16:writable] Error: Parse error: The identifier
        */
      case (path, start, end) ~ tpe ~ msg => CompilerError(tpe, path, start, end, msg)
    }
    | "\"" ~> errorPosition ~ (("Error" | "Warning") <~ ":") ~
      errorSub("\\\"") ^^ {
        case (path, start, end) ~ tpe ~ msg =>
        CompilerError(tpe, path,start,end,msg)
      }
    | "{" ~> "\"" ~> errorPosition ~ (("Error" | "Warning") <~ ":") ~
      errorSub("\\\"") <~ additionalScriptingInfos <~ "}" ^^ {
      case (path, start, end) ~ tpe ~ msg =>
      /** Parses something similar to:
        * {"[/Users/nico/Documents/mo-tests/build.mos:5:1-5:30:writable] Error: Klasse OpenModelica..
        */
        val delimiter = "\""
        if(msg.contains(delimiter)) CompilerError(tpe, path, start, end, msg.substring(0, msg.indexOf(delimiter)))
        else CompilerError(tpe, path, start, end, msg)
    }
  )

  /* parses something similar to: "TRANSLATION", "Error", "3" */
  def additionalScriptingInfos: Parser[List[String]] =
    rep1sep("\"" ~> ident <~ "\"", ",")

  def errorSub(additionalDelimiter:String): Parser[String] =
    rep1sep((not("Error" | "Warning" | additionalDelimiter.r) ~> (word)), "") ^^ {
      case words =>
        println("found words: "+words)
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

  def errorPosition: Parser[(String, FilePosition, FilePosition)] =
    ("["~> path <~ ":") ~ (filePosition <~ "-") ~ filePosition <~ ":" <~ ident <~ "]" ^^ {
      case path ~ start ~ end => (path,start,end)
    }

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
