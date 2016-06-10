package de.thm.moie.server

import akka.stream.ActorMaterializer
import org.scalatest.{ Matchers, WordSpec }
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model._
import akka.util.ByteString
import akka.http.scaladsl.testkit.ScalatestRouteTest
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps
import java.nio.file._

class RoutesSpec extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  val service = new ServerSetup with Routes {
    override lazy val projectsManager: ActorRef = actorSystem.actorOf(Props[ProjectsManagerActor], name = "Root-ProjectsManager")
  }

  val tmpPath = Files.createTempDirectory("moie")
  val projPath = tmpPath.resolve("routes-test")

  override def beforeAll() = {
    Files.createDirectory(projPath)
  }

  override def afterAll() = {
    if(!service.actorSystem.isTerminated) {
      service.actorSystem.terminate()
      fail("ActorSystem didn't terminate as expected through /stop-server!")
    }
  }

  "moie" should {
    val jsonRequest = ByteString(
        s"""
           |{
           |"path":"${projPath.toAbsolutePath()}",
           |"outputDirectory": "target",
           |"compilerFlags": []
           |}
        """.stripMargin)
      val postRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/moie/connect",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

    "return a valid project-id for POST /moie/connect" in {
      postRequest ~> service.routes ~> check {
        responseAs[String] shouldEqual "0"
      }
    }
    "return the same project-id for same project" in {
      postRequest ~> service.routes ~> check {
        responseAs[String] shouldEqual "0"
        postRequest ~> service.routes ~> check {
          responseAs[String] shouldEqual "0"
        }
      }
    }

    "return NoContent for /disconnect with valid project-id" in {
      postRequest ~> service.routes ~> check {
        responseAs[String] shouldEqual "0"
      }

      Post("/moie/project/0/disconnect") ~> service.routes ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }
    "return NoContent for /disconnect with non-valid project-id" in {
      Post("/moie/project/200/disconnect") ~> service.routes ~> check {
        status shouldEqual StatusCodes.NoContent
      }
    }

    "return NotFound for /compile with non-valid project-id" in {
      Post("/moie/project/200/compile") ~> service.routes ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[String] shouldEqual "unknown project-id 200"
      }
    }

    "moie" should {
      "shutdown when calling /stop-server" in {
        Post("/moie/stop-server") ~> service.routes ~> check {
          status shouldEqual StatusCodes.Accepted
        }
        service.actorSystem.awaitTermination(5 seconds)
      }

    }
  }
}
