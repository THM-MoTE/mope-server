package de.thm.moie.templates

import de.thm.moie.templates.TemplateEngine._
import de.thm.moie.utils.IOUtils
import org.scalatest.{Matchers, WordSpec}

class TemplateEngineSpec extends WordSpec with Matchers {
  val is = getClass.getResourceAsStream("/templates/documentation.html")
  val styleIS = getClass.getResourceAsStream("/templates/style.css")
  val fileContent = IOUtils.toString(is)
  val engine = new TemplateEngine(fileContent)
  val styleEngine = new TemplateEngine(IOUtils.toString(styleIS))

  "TemplateEngine" should {
    "replace keys with given values" in {
      val map:Map[String, TemplateValue] = Map(
        "className" -> SimpleValue("Nico"),
        "subcomponents" -> ListValue(List("test-1", "test-2", "test-10")),
        "info-header" -> SimpleValue("header"),
        "info-string" -> SimpleValue("a important string"),
        "revisions" -> SimpleValue("V 1.1"),
        "styles"  -> SimpleValue("there's no style")
      )
      val result = engine.insert(map).getContent
      result.contains("{") shouldBe false
      result.contains("}") shouldBe false
      result.contains("<ul>") shouldBe true
      result.contains("</ul>") shouldBe true
    }

    "merge 2 templates" in {
      val result = engine.merge(styleEngine, "styles").getContent
      result.contains("#main-wrapper") shouldBe true
      result.contains(".horizontal-bar {") shouldBe true
      result.contains("background-color: #80ba24;") shouldBe true
    }

    "concatenate 2 templates" in {
      val templ1 = new TemplateEngine("this is an awesome string from {name}\n").insert(Map("name" -> SimpleValue("nico")))
      val templ2 = new TemplateEngine("this is a second string from {name}").insert(Map("name" -> SimpleValue("tim")))

      val templ3 = templ1 concat templ2
      templ3.getContent shouldBe "this is an awesome string from nico\n" + "this is a second string from tim"
    }
  }
}
