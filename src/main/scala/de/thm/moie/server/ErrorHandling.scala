package de.thm.moie.server

import java.util.NoSuchElementException

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler

trait ErrorHandling {
  this: ServerSetup =>

  def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex:NotFoundException =>
      extractUri { uri =>
        serverlog.debug("got NotFoundExc for {}", uri)
        complete(HttpResponse(StatusCodes.NotFound, entity = ex.msg))
      }
    case ex:NoSuchElementException =>
      extractUri { uri =>
        serverlog.debug("got NoSucheElementExc for {}", uri)
        complete(HttpResponse(StatusCodes.NotFound, entity = ex.getMessage))
      }
    case ex:Exception =>
      extractUri { uri =>
        serverlog.error(s"Error by request $uri {}", ex)
        complete(StatusCodes.InternalServerError)
      }
  }
}
