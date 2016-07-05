/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import akka.stream.ActorMaterializer
import org.scalatest.{Matchers, WordSpec}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.util.ByteString
import akka.http.scaladsl.testkit.ScalatestRouteTest

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps
import java.nio.file._

import de.thm.moie._
import de.thm.moie.compiler.CompilerError
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeoutException

class RoutesSpec extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  val service = new ServerSetup with Routes {
    override def actorSystem = system
    override implicit lazy val materializer:ActorMaterializer = ActorMaterializer()
    override lazy val projectsManager: ActorRef = system.actorOf(Props[ProjectsManagerActor], name = "Root-ProjectsManager")
  }

  val tmpPath = Files.createTempDirectory("moie")
  val projPath = tmpPath.resolve("routes-test")
  val testFile = projPath.resolve("test.mo")
  val testScript = projPath.resolve("test.mos")

  override def beforeAll() = {
    Files.createDirectory(projPath)
  }

  override def afterAll() = {
    super.afterAll()
    removeDirectoryTree(tmpPath)
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

    "return BadRequest with an error description if path in ProjectDescription is faulty" in {
      val faultyJsonRequest = ByteString(
          s"""
             |{
             |"path":"derb",
             |"outputDirectory": "target",
             |"compilerFlags": []
             |}
          """.stripMargin)
        val faultyPostRequest = HttpRequest(
          HttpMethods.POST,
          uri = "/moie/connect",
          entity = HttpEntity(MediaTypes.`application/json`, faultyJsonRequest))
        faultyPostRequest ~> service.routes ~> check {
          responseAs[List[String]] shouldEqual List("derb isn't a directory")
          status shouldEqual StatusCodes.BadRequest
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
      Get("/moie/project/200/compile") ~> service.routes ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[String] shouldEqual "unknown project-id 200"
      }
    }

    "return a json-array with compiler errors for /compile" in {
      val content = """
model test
Rel x
end test;
""".stripMargin

      val bw = Files.newBufferedWriter(testFile, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)
      bw.write(content)
      bw.close()

      Thread.sleep(10000) //wait till buffers are written

      Get("/moie/project/0/compile") ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        val errors = responseAs[List[CompilerError]]
        errors.size shouldBe (1)
      }

      val validContent = """
model test
Real x;
end test;
""".stripMargin

      val bw2 = Files.newBufferedWriter(testFile, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)
      bw2.write(validContent)
      bw2.close()

      Thread.sleep(10000) //wait till buffers are written

      Get("/moie/project/0/compile") ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        val errors = responseAs[List[CompilerError]]
        errors.size shouldBe (0)
      }
    }

        "return a json-array with compiler errors for /compileScript" in {
      val content = """
lodFile("bouncing_ball.mo");
""".stripMargin

      val bw = Files.newBufferedWriter(testScript, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)
      bw.write(content)
      bw.close()

      Thread.sleep(10000) //wait till buffers are written

          val jsonRequest = ByteString(s"""
{ "path": "${testScript.toAbsolutePath}" }
""")

      val postRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/moie/project/0/compileScript",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

       postRequest ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        val errors = responseAs[List[CompilerError]]
        errors.size shouldBe > (0)
      }

      val validContent = """
loadFile("bouncing_ball.mo");
""".stripMargin

      val bw2 = Files.newBufferedWriter(testScript, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)
      bw2.write(validContent)
      bw2.close()

      Thread.sleep(10000) //wait till buffers are written

          val jsonRequest2 = ByteString(s"""
{ "path": "${testScript.toAbsolutePath}" }
""")

                val postRequest2 = HttpRequest(
        HttpMethods.POST,
        uri = "/moie/project/0/compileScript",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest2))


         postRequest2 ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        val errors = responseAs[List[CompilerError]]
        errors.size shouldBe (0)
      }
    }

    "moie" should {
      "shutdown when calling /stop-server" in {
        Post("/moie/stop-server") ~> service.routes ~> check {
          status shouldEqual StatusCodes.Accepted
        }
        try {
          Await.ready(service.actorSystem.whenTerminated, 5 seconds)
        } catch {
          case _:TimeoutException => fail("ActorSystem didn't terminated as expected")
        }
      }
    }
  }
}
