package de.thm.moie.templates

import TemplateEngine._
import scala.language.implicitConversions

class TemplateEngine(fileContent:String) {
  def insert(m:Map[String, _ <: TemplateValue]): TemplateEngine = {
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
}
