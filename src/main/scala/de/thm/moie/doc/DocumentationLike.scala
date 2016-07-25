package de.thm.moie.doc

trait DocumentationLike {
  def getDocumentation(className:String): Option[DocInfo]
}
