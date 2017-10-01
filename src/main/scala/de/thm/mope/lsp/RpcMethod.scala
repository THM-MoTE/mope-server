package de.thm.mope.lsp

import akka.stream.scaladsl._
import de.thm.mope.lsp.messages.RequestMessage
import spray.json.{JsValue, JsonFormat}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class RpcMethod[In:JsonFormat, Out:JsonFormat](
  methodName:String, next:Option[RpcMethod[_,_]] = None)(
  val handler: In => Future[Out]) {

  def |[I2:JsonFormat,O2:JsonFormat](other:RpcMethod[I2,O2]) = {
    RpcMethod(methodName, Some(other))(handler)
  }

  def toFlows(parallelism:Int)(implicit context:ExecutionContext):List[Flow[RequestMessage,Try[JsValue], akka.NotUsed]] = {
    val flow = Flow[RequestMessage].filter { msg =>
      msg.method == methodName
    }.mapAsync(parallelism) { msg =>
      Try(implicitly[JsonFormat[In]].read(msg.params))
        .map(handler) match { //turns Try[Future[JsValue]] => Future[Try[JsValue]] a.k.a. 'sequence'
        case Success(fut) => fut.map(Success.apply)
        case Failure(ex) => Future.successful(Failure(ex))
      }
    }.map(_.map { out =>
      implicitly[JsonFormat[Out]].write(out)
    })
    next match {
      case Some(x) => flow::x.toFlows(parallelism)
      case None => flow::Nil
    }
  }
  def methods:Set[String] = {
    next match {
      case Some(x) => x.methods + methodName
      case None => Set(methodName)
    }
  }
}

object RpcMethod {
  def apply[In:JsonFormat, Out:JsonFormat](methodName:String)(f:In=>Out): RpcMethod[In,Out] = {
    RpcMethod[In,Out](methodName,None)(Future.successful[Out] _ compose f)
  }
}