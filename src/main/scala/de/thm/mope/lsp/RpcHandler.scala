package de.thm.mope.lsp

import akka.stream.scaladsl._
import spray.json.{JsonFormat,JsValue}

case class RpcHandler[In:JsonFormat, Out:JsonFormat](
  methodName:String, next:Option[RpcHandler[_,_]] = None)(
  handler:Flow[In,Out,akka.NotUsed]) {

  def |[I2:JsonFormat,O2:JsonFormat](other:RpcHandler[I2,O2]) = {
    RpcHandler(methodName, Some(other))(handler)
  }

  def toFlows:List[Flow[RpcMsg,JsValue, akka.NotUsed]] = {
    val flow = Flow[RpcMsg].filter { msg =>
      msg.method == methodName
    }.map { msg =>
      implicitly[JsonFormat[In]].read(msg.params)
    }.via(handler)
    .map { out =>
      implicitly[JsonFormat[Out]].write(out)
    }
    next match {
      case Some(x) => flow::x.toFlows
      case None => flow::Nil
    }
  }
}
