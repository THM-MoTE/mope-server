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

package de.thm.mope.suggestion

import de.thm.mope.declaration.JumpToLike
import de.thm.mope.suggestion.Suggestion.Kind

import scala.concurrent.{ExecutionContext, Future}

trait CompletionLike extends JumpToLike {
  def getClasses(className: String): Set[(String, Kind.Value)]

  def getClassesAsync(className: String)(
    implicit context: ExecutionContext): Future[Set[(String, Kind.Value)]] =
    Future(getClasses(className))

  def getParameters(className: String): List[(String, Option[String])]

  def getClassDocumentation(className: String): Option[String]

  def getGlobalScope(): Set[(String, Kind.Value)]
}
