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


package de.thm.mope.server

import java.util.NoSuchElementException

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler

import scala.concurrent.{ExecutionContext, Future}

/** ErrorHandler for Mo|E routes.
  * This interface is triggered if an exception is thrown while
  * handling a HTTP Request.
  */
trait ErrorHandling {

  def serverlog: LoggingAdapter

  def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: NotFoundException =>
      extractUri { uri =>
        serverlog.debug("got NotFoundExc for {}", uri)
        complete(HttpResponse(StatusCodes.NotFound, entity = ex.msg))
      }
    case ex: NoSuchElementException =>
      extractUri { uri =>
        serverlog.debug("got NoSuchElementExc for {}", uri)
        complete(HttpResponse(StatusCodes.NotFound, entity = ex.getMessage))
      }
    case ex: Exception =>
      extractUri { uri =>
        serverlog.error(s"Error by request $uri {}", ex)
        complete(HttpResponse(StatusCodes.InternalServerError, entity = ex.getMessage))
      }
  }

  def optionToNotFoundExc[A](opt: Option[A], excMsg: String)(implicit execContext: ExecutionContext): Future[A] =
    opt match {
      case Some(a) => Future.successful(a)
      case None => Future.failed(new NotFoundException(excMsg))
    }
}
