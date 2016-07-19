package de.thm.moie.templates

import de.thm.moie.templates.TemplateEngine._
import de.thm.moie.utils.IOUtils
import org.scalatest.{Matchers, WordSpec}

class TemplateEngineSpec extends WordSpec with Matchers {
  val is = getClass.getResourceAsStream("/templates/documentation.html")
  val fileContent = IOUtils.toString(is)
  val engine = new TemplateEngine(fileContent)
  "TemplateEngine" should {
    "replace keys with given values" in {
      val map:Map[String, TemplateValue] = Map(
        "className" -> SimpleValue("Nico"),
        "subcomponents" -> ListValue(List("test-1", "test-2", "test-10")),
        "info-header" -> SimpleValue("header"),
        "info-string" -> SimpleValue("a important string"),
        "revisions" -> SimpleValue("V 1.1")
      )
      val result = engine.insert(map)
      result.contains("{") shouldBe false
      result.contains("}") shouldBe false
      result.contains("<ul>") shouldBe true
      result.contains("</ul>") shouldBe true
    }
  }
}
