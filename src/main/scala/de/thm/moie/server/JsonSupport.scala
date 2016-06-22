/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.thm.moie.compiler.{CompilerError, FilePosition}
import de.thm.moie.project.{ProjectDescription, FilePath}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val projectDescriptionFormat: RootJsonFormat[ProjectDescription] = jsonFormat3(ProjectDescription)
  implicit val filePositionFormat:RootJsonFormat[FilePosition] = jsonFormat2(FilePosition)
  implicit val compileErrorFormat:RootJsonFormat[CompilerError] = jsonFormat5(CompilerError)
  implicit val filePathFormat:RootJsonFormat[FilePath] = jsonFormat1(FilePath)
}
