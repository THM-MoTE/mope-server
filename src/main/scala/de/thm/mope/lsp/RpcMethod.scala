package de.thm.mope.lsp

import akka.stream.scaladsl._
import de.thm.mope.lsp.messages.RpcMessage
import spray.json.{JsValue, JsonReader, JsonWriter}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class RpcMethod[In:JsonReader, Out:JsonWriter] private[lsp] (
  methodName:String, next:Option[RpcMethod[_,_]] = None)(
  val handler: In => Future[Out]) {

  def ||[I2:JsonReader,O2:JsonWriter](other:RpcMethod[I2,O2]):RpcMethod[In,Out] = {
    // RpcMethod(other.methodName, Some(this))(other.handler)
    val newNext = next.map(_ || other).getOrElse(other)
    RpcMethod(this.methodName, Some(newNext))(this.handler)
  }

  def toFlows(parallelism:Int)(implicit context:ExecutionContext):List[Flow[RpcMessage,Try[JsValue], akka.NotUsed]] = {
    val flow = Flow[RpcMessage].filter { msg =>
      msg.method == methodName
    }.mapAsync(parallelism) { msg =>
      Try(implicitly[JsonReader[In]].read(msg.params))
        .map(handler) match { //turns Try[Future[JsValue]] => Future[Try[JsValue]] a.k.a. 'sequence'
        case Success(fut) => fut.map(Success.apply)
        case Failure(ex) => Future.successful(Failure(ex))
      }
    }.map(_.map { out =>
      implicitly[JsonWriter[Out]].write(out)
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
  def notification[In:JsonReader](methodName:String)(f:In=>Future[Unit])(implicit unitWriter:JsonWriter[Unit]): RpcMethod[In,Unit] = RpcMethod(methodName, None)(f)
  def request[In:JsonReader, Out:JsonWriter](methodName:String)(f:In=>Future[Out]): RpcMethod[In,Out] = RpcMethod(methodName, None)(f)
}
