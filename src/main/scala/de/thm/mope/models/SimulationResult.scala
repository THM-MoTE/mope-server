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

case class SimulationResult(modelName:String, variables:Map[String,Seq[Double]])
