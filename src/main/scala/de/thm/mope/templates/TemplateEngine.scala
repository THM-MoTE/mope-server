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

import java.io.InputStream

import de.thm.mope.templates.TemplateEngine._
import de.thm.mope.utils.IOUtils

import scala.language.implicitConversions

class TemplateEngine(fileContent:String) {
  def insert(m:Map[String,TemplateValue]): TemplateEngine = {
    new TemplateEngine(m.foldLeft(fileContent) {
      case (acc, (key, ListValue(v))) =>
        acc.replace(fileKey(key), "<ul>" + v.mkString("\n") + "</ul>")
      case (acc, (key, SimpleValue(v))) =>
        acc.replace(fileKey(key), v)
    })
  }

  def merge(other:TemplateEngine, positionKey:String): TemplateEngine =
    insert(Map(positionKey -> SimpleValue(other.getContent)))

  def concat(other:TemplateEngine): TemplateEngine =
    new TemplateEngine(fileContent + other.getContent)

  def getContent: String = fileContent
  private def fileKey(k:String):String = "{" + k + "}"
}

object TemplateEngine {
  sealed trait TemplateValue
  case class SimpleValue(v:String) extends TemplateValue
  case class ListValue[A](v:List[A]) extends TemplateValue

  implicit def stringToValue(s:String): SimpleValue = SimpleValue(s)
  implicit def listToValue[A](xs:List[A]): ListValue[A] = ListValue(xs)

  def apply(is:InputStream):TemplateEngine = new TemplateEngine(IOUtils.toString(is))
  def merge(engine:TemplateEngine, other: (TemplateEngine,String)*):TemplateEngine = {
    other.foldLeft(engine) { case (engine, (en2, key)) => engine.merge(en2,key) }
  }
}
