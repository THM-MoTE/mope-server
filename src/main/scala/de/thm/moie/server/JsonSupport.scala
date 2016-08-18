/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.thm.moie.compiler.CompilerError
import de.thm.moie.declaration.DeclarationRequest
import de.thm.moie.doc.ClassComment
import de.thm.moie.position.{FilePath, FilePosition, FileWithLine}
import de.thm.moie.project._
import de.thm.moie.suggestion.CompletionResponse.CompletionType
import de.thm.moie.suggestion.{CompletionRequest, CompletionResponse}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val projectDescriptionFormat: RootJsonFormat[ProjectDescription] = jsonFormat3(ProjectDescription.apply)
  implicit val filePositionFormat:RootJsonFormat[FilePosition] = jsonFormat2(FilePosition)
  implicit val compileErrorFormat:RootJsonFormat[CompilerError] = jsonFormat5(CompilerError)
  implicit val filePathFormat:RootJsonFormat[FilePath] = jsonFormat1(FilePath.apply)
  implicit val fileWithLineFormat:RootJsonFormat[FileWithLine] = jsonFormat2(FileWithLine)
  implicit val completionRequestFormat:RootJsonFormat[CompletionRequest] = jsonFormat3(CompletionRequest)
  implicit val completionTypeFormat = new RootJsonFormat[CompletionType.Value] {
    override def write(tpe:CompletionType.Value) = JsString(tpe.toString)
    override def read(value:JsValue) = value match {
      case JsString(str) => CompletionType.withName(str)
      case _ => throw new DeserializationException("CompletionType.Value expected")
    }
  }
  implicit val completionResponseFormat:RootJsonFormat[CompletionResponse] = jsonFormat4(CompletionResponse.apply)
  implicit val declarationRequestFormat:RootJsonFormat[DeclarationRequest] = jsonFormat1(DeclarationRequest)
  implicit val classCommentFormat:RootJsonFormat[ClassComment] = jsonFormat2(ClassComment)
}
