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

import akka.stream.scaladsl.Flow
import de.thm.mope.utils.StreamUtils
import org.apache.commons.lang3.StringUtils

class PrefixMatcher(prefix: String) {
  def startsWith(negate: Boolean = false) = {
    val pred = { suggestion: Suggestion =>
      suggestion.name.startsWith(prefix)
    }
    if (negate) Flow[Suggestion].filterNot(pred)
    else Flow[Suggestion].filter(pred)
  }

  /** Returns Seq[Suggestion] sorted by their closest distance to `prefix`. */
  def levenshtein =
    Flow[Suggestion].via(StreamUtils.seq).map { seq =>
      seq.map { suggestion =>
        //map to (distance -> suggestion)
        val (objectName, suffixMember) = sliceAtLastDot(prefix)
        val (suggestionObjectName, suggestionMember) = sliceAtLastDot(suggestion.name)
        (if (!suffixMember.isEmpty) {
          //match on suffix of .
          StringUtils.getLevenshteinDistance(suggestionMember, suffixMember).intValue()
        } else {
          //match on prefix of .
          StringUtils.getLevenshteinDistance(suggestionObjectName, objectName).intValue()
        }, suggestion)
      }.sortBy { case (v, _) => v } //sort by their distance
        .collect { //only elements that contain the prefix
        case (v, suggestion) if v < suggestion.name.size => suggestion
      }
    }
}
