package de.thm.moie.compiler

import de.thm.moie.project.DocInfo

trait DocumentationLike {
  def getDocumentation(className:String): Option[DocInfo]
}
