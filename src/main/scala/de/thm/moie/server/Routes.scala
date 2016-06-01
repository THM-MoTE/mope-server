package de.thm.moie.server

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.sys.process._
import scala.concurrent.Future
import java.io._

import akka.actor.Props
import de.thm.moie.project.ProjectDescription

trait Routes extends JsonSupport {
  val routes =
    pathPrefix("moie") {
      path("connect") {
        post {
          entity(as[ProjectDescription]) { description =>
            println(description)
            complete("subscribed")
          }
        } ~ get {
          complete(ProjectDescription("Dummy-URL", List()))
        }
      }
    }
}
