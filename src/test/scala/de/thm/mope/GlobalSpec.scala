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

package de.thm.mope

import java.net.URL

import org.scalatest._

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

  "ApplicationMode.parseString" should "parse mode options" in {
    val erg1 = Global.ApplicationMode.parseString("dev")
    val erg2 = Global.ApplicationMode.parseString("DEVELOPMENT")
    val erg3 = Global.ApplicationMode.parseString("prod")
    val erg4 = Global.ApplicationMode.parseString("PRODUCTION")

    erg1 shouldBe Global.ApplicationMode.Development
    erg2 shouldBe Global.ApplicationMode.Development
    erg3 shouldBe Global.ApplicationMode.Production
    erg4 shouldBe Global.ApplicationMode.Production
  }

  an [IllegalArgumentException] should be thrownBy {
    Global.ApplicationMode.parseString("dav")
  }
}
