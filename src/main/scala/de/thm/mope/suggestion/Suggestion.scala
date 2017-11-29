/**
  * Copyright (C) 2016,2017 Nicola Justus <nicola.justus@mni.thm.de>
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


package de.thm.mope.suggestion

import de.thm.mope.suggestion.Suggestion._

case class Suggestion(kind: Kind.Value,
                      name: String,
                      parameters: Option[Seq[String]],
                      classComment: Option[String],
                      `type`: Option[String]) {
  def displayString: String =
    s"$kind - $name"
}

object Suggestion {

  object Kind extends Enumeration {
    val Type, Variable, Function, Keyword, Package, Model, Class, Property = Value
  }

}
