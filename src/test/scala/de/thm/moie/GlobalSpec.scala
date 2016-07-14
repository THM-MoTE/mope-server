package de.thm.moie

import org.scalatest._
import java.net.URL

class GlobalSpec extends FlatSpec with Matchers {
  def rsc(path:String):URL =
    getClass.getResource(path).toURI().toURL()

  "`readValuesFromResource`" should "return a list of words" in {
    val words = Global.readValuesFromResource(rsc("/completion/keywords.conf"))(!_.isEmpty)
    words.size shouldBe >= (59)

    words.head shouldBe "algorithm"
    words(1) shouldBe "and"

    val words2 = Global.readValuesFromResource(rsc("/completion/types.conf"))(!_.isEmpty)
    words2.size shouldBe (4)
    words2.head shouldBe "Boolean"
    words2(1) shouldBe "Integer"
  }
}
