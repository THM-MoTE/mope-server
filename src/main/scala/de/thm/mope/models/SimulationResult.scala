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

package de.thm.mope.models

import de.thm.mope.compiler.SimulationError
import spray.json.{DeserializationException, JsNumber, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class SimulateRequest(modelName:String, options:Map[String,JsValue]) {
  def convertOptions(implicit context:ExecutionContext): Future[Map[String,String]] =
    Future.sequence(options.map {
      case (k, JsNumber(i)) => Future.successful(k -> i.toString)
      case (k, JsString(str)) => Future.successful(k -> s"""\"$str\"""")
      case _ => Future.failed(DeserializationException("can't handle options that aren't strings or numbers"))
    })
    .map(_.toMap)
}

case class SimulationResult(modelName:String, variables:Map[String,Seq[Double]], warning:Option[String]=None)
