package de.thm.moie.server

import java.util.NoSuchElementException

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler

trait ErrorHandling {
  this: ServerSetup =>

  def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex:NotFoundException => complete(HttpResponse(StatusCodes.NotFound, entity = ex.msg))
    case ex:NoSuchElementException => complete(HttpResponse(StatusCodes.NotFound, entity = ex.getMessage))
    case ex:Exception =>
      extractUri { uri =>
        serverlog.error(s"Error by request $uri {}", ex)
        complete(StatusCodes.InternalServerError)
      }
  }
}
