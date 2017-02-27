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

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import org.apache.commons.lang3.StringUtils

class PrefixMatcher(prefix:String)(implicit mat: ActorMaterializer) {
  def startsWith =
    Flow[Suggestion].filter { suggestion =>
      suggestion.name.startsWith(prefix)
    }

  def levenshtein =
    Flow[Suggestion].filter { suggestion =>
      val (objectName, suffixMember) = sliceAtLastDot(prefix)
      val (suggestionObjectName, suggestionMember) = sliceAtLastDot(suggestion.name)
      if(!suffixMember.isEmpty) {
        //match on suffix of .
        distance(suggestionMember, suffixMember)
      }
      else if(!objectName.isEmpty) {
        //match on prefix of .
        distance(suggestionObjectName, objectName)
      }
      else false
    }

  private def distance(str1:String,str2:String): Boolean =
    StringUtils.getLevenshteinDistance(str1, str2) < Math.min(5, str1.length / 2)
}
