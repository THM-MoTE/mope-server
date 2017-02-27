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

package de.thm.mope.server

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{server, unmarshalling}
import de.thm.mope.position.FilePath

import scala.concurrent.Future

trait EnsembleRoutes {
  this: ServerSetup with JsonSupport with ErrorHandling =>

  def ensembleHandler: EnsembleHandler

  def ensembleRoutes =
    pathPrefix("ensemble") {
      (post & entity(as[FilePath])) { filepath =>
        path("move") {
          ensembleHandler.openInMove(filepath.path) match {
            case Right(_) => complete(StatusCodes.NoContent)
            case Left(err) => complete(HttpResponse(StatusCodes.BadRequest, entity = err))
        }
      }
    }
  }
}