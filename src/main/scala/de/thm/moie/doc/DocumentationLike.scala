package de.thm.moie.doc

trait DocumentationLike {
  /** Returns the documentation of `className` if the documentation exists. */
  def getDocumentation(className:String): Option[DocInfo]
  /** Returns the class documentation comment of `className` if it exists. */
  def getClassComment(className:String): Option[String]
}
