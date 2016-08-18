package de.thm.moie.server

import java.util.NoSuchElementException

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler

import scala.concurrent.{ExecutionContext, Future}

/** ErrorHandler for Mo|E routes.
  * This interface is triggered if an exception is thrown while
  * handling a HTTP Request.
  */
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
        complete(HttpResponse(StatusCodes.InternalServerError, entity=ex.getMessage))
      }
  }

  def optionToNotFoundExc[A](opt:Option[A], excMsg:String)(implicit execContext: ExecutionContext): Future[A] =
    opt match {
      case Some(a) => Future.successful(a)
      case None => Future.failed(new NotFoundException(excMsg))
    }
}
