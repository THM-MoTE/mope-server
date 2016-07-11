/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import scala.language.postfixOps
import scala.util.parsing.combinator.{ImplicitConversions, RegexParsers}

/** A helper trait containing common-parsers for parsing compiler-errors.
  */
trait CommonParsers extends RegexParsers with ImplicitConversions {
  // regex from: http://stackoverflow.com/a/5954831
  override val whiteSpace = """(\s|//.*|(?m)/\*(\*(?!/)|[^*])*\*/)+""".r

  def unknownPosition:FilePosition = FilePosition(0,0)
  def unknownError:String = "Compiler didn't provide any further message"
  def unknownPath:String = ""

  def pathDelimiter = """/|\\""".r
  def pathIdent = """[a-z-A-Z-.+<>0-9?=_ ]+""".r
  def number = """[0-9]+""".r
  def ident =  """[a-zA-Z0-9]+""".r
  def character = "[a-zA-Z]".r

  /** Parses a path similar to: /home/user/awesome/project.txt */
  def path:Parser[String] =
    (root ?) ~ rep1sep(pathIdent, pathDelimiter) ^^ {
      case None ~ lst => lst.mkString("/")
      case Some(root) ~ lst => root + lst.mkString("/")
    }

  /** Parses the root-node of a filesystem. In c:\ in Windows, / in Linux */
  def root: Parser[String] =
    ("/" //unix style
    | character ~ ":" ~ "\\" ^^ { case ch ~ _ ~ _ => ch +":\\" } //windows style
    )
}
