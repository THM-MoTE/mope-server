package de.thm.moie.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.thm.moie.project.ProjectDescription
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val projectDescriptionFormat = jsonFormat2(ProjectDescription)
}
