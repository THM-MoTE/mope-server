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

package de.thm.mope.utils

import akka.stream._
import akka.stream.scaladsl._

object StreamUtils {
  val numbers: Source[Int, _] = Source.unfold(0) { n => Some(n+1 -> n)}

  def broadcastAll[In,Out,Mat](xs:List[Flow[In,Out,Mat]]):Flow[In,Out,akka.NotUsed] = Flow.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[akka.NotUsed] =>
      import GraphDSL.Implicits._
      val bcast = builder.add(Broadcast[In](xs.size))
      val merge = builder.add(Merge[Out](xs.size))

      for (flow <- xs) {
        bcast ~> flow ~> merge
      }
      FlowShape(bcast.in, merge.out)
  })
}
