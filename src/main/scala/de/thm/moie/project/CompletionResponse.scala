package de.thm.moie.project

import CompletionResponse._
case class CompletionResponse(completionType: CompletionType.Value,
                              name:String,
                              parameters:Option[Seq[String]],
                              classComment: Option[String])

object CompletionResponse {
  object CompletionType extends Enumeration {
    val Type, Variable, Function, Keyword, Package, Model, Class = Value
  }
}
