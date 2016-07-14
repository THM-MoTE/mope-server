/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import de.thm.moie.project.CompletionResponse.CompletionType
import scala.concurrent.{ExecutionContext, Future}

trait CompletionLike {
  def getClasses(className:String): Set[(String, CompletionType.Value)]
  def getClassesAsync(className:String)(
    implicit context:ExecutionContext): Future[Set[(String, CompletionType.Value)]] =
      Future(getClasses(className))

  def getParameters(className:String): List[(String, Option[String])]
  def getClassDocumentation(className:String): Option[String]
  def getGlobalScope(): Set[(String, CompletionType.Value)]
}
