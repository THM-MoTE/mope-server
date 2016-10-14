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

package de.thm.mope.doc

import de.thm.mope.doc.DocInfo._

/** The documentation of a modelica class. */
case class DocInfo(info:String,
                   revisions:String,
                   infoHeader:String,
                   subcomponents:Set[Subcomponent])

object DocInfo {
  /** A short information about a subcomponent inside a modelica class. */
  case class Subcomponent(className:String, classComment:Option[String]) extends Ordered[Subcomponent] {
    override def compare(that: Subcomponent): Int = className compare that.className
  }

  /** Defines the ordering for Subcomponent  based on their `className` */
  implicit val subcomponentOrdering = new Ordering[Subcomponent] {
    override def compare(x: Subcomponent, y: Subcomponent): Int = x compare y
  }
}
