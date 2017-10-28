package de.thm.mope.templates

import com.softwaremill.tagging._
import de.thm.mope.tags.{DocMarker, MissingDocMarker}
import de.thm.mope.utils.IOUtils

trait TemplateModule {
  import TemplateModule._
  private val cssStream = getClass.getResourceAsStream("/templates/style.css")
  private val docStream = getClass.getResourceAsStream("/templates/documentation.html")
  private val missingDocStream = getClass.getResourceAsStream("/templates/missing-doc.html")
  private val styleEngine = new TemplateEngine(IOUtils.toString(cssStream))
  val docEngine:DocTemplate = new TemplateEngine(IOUtils.toString(docStream)).merge(styleEngine, "styles").taggedWith[DocMarker]
  val missingDocEngine:MissingDocTemplate = new TemplateEngine(IOUtils.toString(missingDocStream)).merge(styleEngine, "styles").taggedWith[MissingDocMarker]
}

object TemplateModule {
  type DocTemplate = TemplateEngine@@DocMarker
  type MissingDocTemplate = TemplateEngine@@MissingDocMarker
}