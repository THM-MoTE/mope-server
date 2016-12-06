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

  def containFn(fn:String):String = s"contain a `$fn` implementation"

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

    containFn("map") in {
      val tree = Node(5, List(Node(2, List(Leaf(20), Leaf(30))), Leaf(5), Leaf(10)))
      tree.map(_*2) shouldBe Node(10, List(Node(4, List(Leaf(40), Leaf(60))), Leaf(10), Leaf(20)))

      val tree2 = Node("nico", List(Node("bella", List(Leaf("sandra"), Leaf("sarah")))))
      tree2.map(_.toUpperCase) shouldBe Node("NICO", List(Node("BELLA", List(Leaf("SANDRA"), Leaf("SARAH")))))
      tree2.map(_.length) shouldBe Node(4, List(Node(5, List(Leaf(6), Leaf(5)))))
    }

    containFn("fold") in {
      val tree = Node(5, List(Node(2, List(Leaf(20), Leaf(30))), Leaf(5), Leaf(10)))
      val sum = tree.fold(0)(_+_)

      sum shouldBe (5+2+20+30+5+10)
      val diff = tree.fold(100)(_-_)
      diff shouldBe (100-5-2-20-30-5-10)

      val lst = tree.fold(List[Int]()) {
        case (xs, el) => el :: xs
      }
      lst shouldBe List(5,2,20,30,5,10).reverse
    }

    containFn("find") in {
      val tree = Node(5, List(Node(2, List(Leaf(20), Leaf(30))), Leaf(5), Leaf(10)))
      tree.find(_==30) shouldBe Some(30)
      tree.find(_>30) shouldBe None
      tree.find{x => x > 5 && x < 12} shouldBe Some(10)
    }
    containFn("contains") in {
      val tree = Node(5, List(Node(2, List(Leaf(20), Leaf(30))), Leaf(5), Leaf(10)))
      tree.contains(5) shouldBe true
      tree.contains(30) shouldBe true
      tree.contains(-5) shouldBe false
    }

    containFn("filterElements") in {
      val tree = Node(5, List(Node(2, List(Leaf(20), Leaf(30))), Leaf(5), Leaf(10)))
      tree.filterElements(_>=10) shouldBe List(20,30,10)
      tree.filterElements(_<10) shouldBe List(5,2,5)
      tree.filterElements(_>20) shouldBe List(30)
      tree.filterElements(_>30) shouldBe Nil
    }

    containFn("size") in {
      val tree = Node(5, List(Node(2, List(Leaf(20), Leaf(30))), Leaf(5), Leaf(10)))
      tree.size shouldBe 6
      val tree2 = Leaf(10)
      tree2.size shouldBe 1
      val tree3 = Node(5, List())
      tree3.size shouldBe 1
    }
  }
}
