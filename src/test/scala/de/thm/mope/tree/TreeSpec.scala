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

package de.thm.mope.tree

import org.scalatest.{Matchers, WordSpec}
import scala.collection.mutable

class TreeSpec extends WordSpec with Matchers {
  "A tree" should {
    "contain elements" in {
      val tree = Leaf(5)
      val tree2 = Node(5, List(Node(2, List(Leaf(20), Leaf(30))), tree, Leaf(10)))
      val lst = List(5, 2, 20, 30, 5, 10)
      tree.foreach(_ shouldBe 5)
      val buffer = mutable.ListBuffer[Int]()
      tree2.foreach(buffer.+=)
      buffer.toList shouldBe lst
    }
    "always visit elements in the same order" in {
      val tree = Node(10,
        List(Node(20, List(Leaf(30))), Leaf(50)))
      val buffer = mutable.ListBuffer[Int]()
      tree.foreach(buffer.+=)

      val elemLists = (0 until 20).map { _ =>
        val buf = mutable.ListBuffer[Int]()
        tree.foreach(buf.+=)
        buf.toList
      }
      elemLists.foreach(_ shouldBe buffer.toList)
    }

    "pretty print its elements" in {
      val tree = Node(5, List(Node(2, List(Leaf(20), Leaf(30))), Leaf(5), Leaf(10)))
      val str = """- 5
|  - 2
|    - 20
|    - 30
|  - 5
|  - 10""".stripMargin

      tree.pretty shouldBe str

      tree.pretty("*") shouldBe str.replaceAll("-", "*")
    }
  }
}
