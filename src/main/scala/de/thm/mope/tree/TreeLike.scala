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

sealed trait TreeLike[+Elem] {
  def foreach[U](f: Elem => U): Unit
  def fold[Z](zero:Z)(op: (Z, Elem) => Z): Z = ???
  def map[B >: Elem](f: Elem => B): TreeLike[B] = this match {
    case Leaf(e) => Leaf(f(e))
    case Node(e, children) =>
      val newChilds = children.map(_.map(f))
      Node(f(e), newChilds)
  }
  def pretty:String = pretty()
  def pretty(indicator:String="-"):String = {
    def spaces(implicit indent:Int):String = " "*indent+indicator+" "
    def mkStr(elem:TreeLike[Elem])(implicit indent:Int): String = {
      elem match {
        case Leaf(e) => spaces+e.toString
        case Node(e, children) => spaces + e+"\n"+ children.map(mkStr(_)(indent+2)).mkString("\n")
      }
    }
    mkStr(this)(0)
  }
}

case class Node[+Elem](elem:Elem, children:List[TreeLike[Elem]]) extends TreeLike[Elem] {
  override def foreach[U](f: Elem => U): Unit = {
    f(elem)
    children.foreach(_.foreach(f))
  }
}
case class Leaf[+Elem](elem:Elem) extends TreeLike[Elem] {
  override def foreach[U](f: Elem => U): Unit = f(elem)
}
