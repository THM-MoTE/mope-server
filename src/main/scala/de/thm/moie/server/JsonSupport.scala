package de.thm.moie.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.thm.moie.compiler.CompilerError
import de.thm.moie.project.ProjectDescription
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val projectDescriptionFormat: RootJsonFormat[ProjectDescription] = jsonFormat3(ProjectDescription)
  implicit val compileErrorFormat:RootJsonFormat[CompilerError] = jsonFormat4(CompilerError)
}
