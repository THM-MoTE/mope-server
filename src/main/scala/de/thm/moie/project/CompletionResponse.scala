package de.thm.moie.project

import CompletionResponse._
case class CompletionResponse(completionType: CompletionType.Value,
                              name:String,
                              parameters:Option[Seq[String]])

object CompletionResponse {
  object CompletionType extends Enumeration {
    val Type, Variable, Function, Keyword = Value
  }
}
