/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.thm.moie.compiler.{CompilerError, FilePosition}
import de.thm.moie.project.CompletionResponse.CompletionType
import de.thm.moie.project.{CompletionRequest, CompletionResponse, FilePath, ProjectDescription}
import spray.json.{DefaultJsonProtocol, DeserializationException, RootJsonFormat, JsString, JsValue}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val projectDescriptionFormat: RootJsonFormat[ProjectDescription] = jsonFormat4(ProjectDescription.apply)
  implicit val filePositionFormat:RootJsonFormat[FilePosition] = jsonFormat2(FilePosition)
  implicit val compileErrorFormat:RootJsonFormat[CompilerError] = jsonFormat5(CompilerError)
  implicit val filePathFormat:RootJsonFormat[FilePath] = jsonFormat1(FilePath.apply)
  implicit val completionRequestFormat:RootJsonFormat[CompletionRequest] = jsonFormat3(CompletionRequest)
  implicit val completionTypeFormat = new RootJsonFormat[CompletionType.Value] {
    override def write(tpe:CompletionType.Value) = JsString(tpe.toString)
    override def read(value:JsValue) = value match {
      case JsString(str) => CompletionType.withName(str)
      case _ => throw new DeserializationException("CompletionType.Value expected")
    }
  }
  implicit val completionResponseFormat:RootJsonFormat[CompletionResponse] = jsonFormat4(CompletionResponse.apply)
}
