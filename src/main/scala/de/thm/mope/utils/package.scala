package de.thm.mope

import de.thm.mope.server.NotFoundException

import scala.concurrent.{ExecutionContext, Future}

package object utils {
  def optionToNotFoundExc[A](opt:Option[A], excMsg:String)(implicit execContext: ExecutionContext): Future[A] =
    opt match {
      case Some(a) => Future.successful(a)
      case None => Future.failed(new NotFoundException(excMsg))
    }
}
