package de.thm.moie.doc

trait DocumentationLike {
  /** Returns the documentation of `className` if the documentation exists. */
  def getDocumentation(className:String): Option[DocInfo]
}
