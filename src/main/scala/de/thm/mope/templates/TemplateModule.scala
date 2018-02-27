/**
  * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */

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
  val docEngine: DocTemplate = new TemplateEngine(IOUtils.toString(docStream)).merge(styleEngine, "styles").taggedWith[DocMarker]
  val missingDocEngine: MissingDocTemplate = new TemplateEngine(IOUtils.toString(missingDocStream)).merge(styleEngine, "styles").taggedWith[MissingDocMarker]
}

object TemplateModule {
  type DocTemplate = TemplateEngine @@ DocMarker
  type MissingDocTemplate = TemplateEngine @@ MissingDocMarker
}
