package de.thm.moie.templates

import TemplateEngine._

class TemplateEngine(fileContent:String) {
  def insert(m:Map[String, _ <: TemplateValue]): String = {
    m.foldLeft(fileContent) {
      case (acc, (key, ListValue(v))) =>
        acc.replace(fileKey(key), "<ul>" + v.mkString("\n") + "</ul>")
      case (acc, (key, SimpleValue(v))) =>
        acc.replace(fileKey(key), v)
    }
  }

  private def fileKey(k:String):String = "{" + k + "}"
}

object TemplateEngine {
  sealed trait TemplateValue
  case class SimpleValue(v:String) extends TemplateValue
  case class ListValue[A](v:List[A]) extends TemplateValue
}
