/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.moie.server

import java.nio.file._
import java.util.concurrent.TimeoutException

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.util.ByteString
import de.thm.moie._
import de.thm.moie.compiler.CompilerError
import de.thm.moie.declaration.DeclarationRequest
import de.thm.moie.doc.ClassComment
import de.thm.moie.position.{FilePath, FilePosition, FileWithLine}
import de.thm.moie.suggestion.CompletionResponse.CompletionType
import de.thm.moie.suggestion.{CompletionRequest, CompletionResponse}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class RoutesSpec extends WordSpec with Matchers with ScalatestRouteTest with JsonSupport {
  val service = new ServerSetup with Routes {
    override def actorSystem = system
    override implicit lazy val materializer:ActorMaterializer = ActorMaterializer()
    override lazy val projectsManager: ActorRef = system.actorOf(Props[ProjectsManagerActor], name = "Root-ProjectsManager")
  }

  val tmpPath = Files.createTempDirectory("moie")
  val projPath = tmpPath.resolve("routes-test")

  val timeout:Duration = 3 seconds

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
           |"outputDirectory": "target"
           |}
        """.stripMargin)
      val postRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/moie/connect",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

    "return a valid project-id for POST /moie/connect" in {
      postRequest ~> service.routes ~> check {
        Thread.sleep(2000)
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
             |"outputDirectory": "target"
             |}
          """.stripMargin)
        val faultyPostRequest = HttpRequest(
          HttpMethods.POST,
          uri = "/moie/connect",
          entity = HttpEntity(MediaTypes.`application/json`, faultyJsonRequest))
        faultyPostRequest ~> service.routes ~> check {
          responseAs[String] shouldEqual "`derb` doesn't exist"
          status shouldEqual StatusCodes.BadRequest
        }
    }

    "return NoContent for /disconnect with valid project-id" in {
      //first connect
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
      val compileRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/moie/project/200/compile",
        entity = HttpEntity(MediaTypes.`application/json`, filePathFormat.write(FilePath("empty")).compactPrint))
      compileRequest ~> service.routes ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[String] shouldEqual "unknown project-id 200"
      }
    }

    "return NotFound for /compileScript with non-valid project-id" in {
      val compileRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/moie/project/200/compileScript",
        entity = HttpEntity(MediaTypes.`application/json`, filePathFormat.write(FilePath("empty")).compactPrint))
      compileRequest ~> service.routes ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[String] shouldEqual "unknown project-id 200"
      }
    }

    "return keyword completions for /completion" in {
      val complReq = CompletionRequest("unknown", FilePosition(0,0), "an")
      val exp = Set("annotation", "and").map(CompletionResponse(CompletionType.Keyword, _, None, None))
      val completionRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/moie/project/0/completion",
        entity = HttpEntity(MediaTypes.`application/json`, completionRequestFormat.write(complReq).compactPrint))
      completionRequest ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Set[CompletionResponse]] shouldBe exp
      }
    }

    "return type completions for /completion" in {
      val complReq = CompletionRequest("unknown", FilePosition(0,0), "Int")
      val exp = Set("Integer").map(CompletionResponse(CompletionType.Type, _, None, None))
      val completionRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/moie/project/0/completion",
        entity = HttpEntity(MediaTypes.`application/json`, completionRequestFormat.write(complReq).compactPrint))
      completionRequest ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Set[CompletionResponse]] shouldBe exp
      }
    }

    "return package completions for /completion" in {
      val complReq = CompletionRequest("unknown", FilePosition(0,0), "Modelica.Electrical.")
      val exp = Set(
        "Modelica.Electrical.Analog" -> "Library for analog electrical models",
        "Modelica.Electrical.Digital" -> "Library for digital electrical components based on the VHDL standard with 9-valued logic and conversion to 2-,3-,4-valued logic",
        "Modelica.Electrical.Machines" -> "Library for electric machines",
        "Modelica.Electrical.MultiPhase" -> "Library for electrical components with 2, 3 or more phases",
        "Modelica.Electrical.QuasiStationary" -> "Library for quasi-stationary electrical singlephase and multiphase AC simulation",
        "Modelica.Electrical.Spice3" -> "Library for components of the Berkeley SPICE3 simulator").
        map {
          case (name, classComment) => CompletionResponse(CompletionType.Package, name, None, Some(classComment))
        }

      val completionRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/moie/project/0/completion",
        entity = HttpEntity(MediaTypes.`application/json`, completionRequestFormat.write(complReq).compactPrint))
      completionRequest ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Set[CompletionResponse]] shouldBe exp
      }
    }

    "return corresponding file for a classname" in {
      val uri = "/moie/project/0/declaration?class=Modelica.Electrical"
      Get(uri) ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[FileWithLine] shouldBe FileWithLine("/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/Electrical/package.mo", 1)
      }

      val uri2 = "/moie/project/0/declaration?class=Modelica.Electrical.Analog"
      Get(uri2) ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[FileWithLine] shouldBe FileWithLine("/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/Electrical/Analog/package.mo", 1)
      }

      val faultyUri = "/moie/project/0/declaration?class=Modelica.nico"
      Get(faultyUri) ~> service.routes ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[String] shouldBe "class Modelica.nico not found"
      }
    }

    "return documentation for a classname" in {
      Get("/moie/project/0/doc?class=Modelica.Electrical") ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String].contains("Documentation for Modelica.Electrical") shouldBe true
      }

      Get("/moie/project/0/doc?class=nico") ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String].contains("The documentation for nico is missing.") shouldBe true
      }
    }

    "return classComment for a classname" in {
      Get("/moie/project/0/comment?class=Modelica.Electrical") ~> service.routes ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[ClassComment] shouldEqual ClassComment("Modelica.Electrical", "Library of electrical models (analog, digital, machines, multi-phase)")
      }

      Get("/moie/project/0/comment?class=Modelica.nico") ~> service.routes ~> check {
        status shouldEqual StatusCodes.NotFound
        responseAs[String] shouldEqual "comment for Modelica.nico not found"
      }
    }


        "return errors for /compile" in {
          val invalidFile = createInvalidFile(projPath)
          val compileRequest = HttpRequest(
            HttpMethods.POST,
            uri = "/moie/project/0/compile",
            entity = HttpEntity(MediaTypes.`application/json`, filePathFormat.write(FilePath(invalidFile.toString)).compactPrint))
          Thread.sleep(8000) //wait till CREATE_EVENT is received (note: MacOS seems to be slow in publishing events)
          compileRequest ~> service.routes ~> check {
            status shouldEqual StatusCodes.OK
            responseAs[List[CompilerError]].size should be >= (1)
          }

          Files.delete(invalidFile)
          val validFile = createValidFile(projPath)

          val compileRequest2 = HttpRequest(
            HttpMethods.POST,
            uri = "/moie/project/0/compile",
            entity = HttpEntity(MediaTypes.`application/json`, filePathFormat.write(FilePath(validFile.toString)).compactPrint))

          compileRequest2 ~> service.routes ~> check {
            Thread.sleep(5000)
            status shouldEqual StatusCodes.OK
            responseAs[List[CompilerError]] shouldBe empty
          }
        }

        "return errors for /compileScript" in {
          val invalidScript = createInvalidScript(projPath)
          val compileRequest = HttpRequest(
            HttpMethods.POST,
            uri = "/moie/project/0/compileScript",
            entity = HttpEntity(MediaTypes.`application/json`, filePathFormat.write(FilePath(invalidScript.toString)).compactPrint))
          Thread.sleep(4000) //wait till CREATE_EVENT is received (note: MacOS seems to be slow in publishing events)
          compileRequest ~> service.routes ~> check {
            status shouldEqual StatusCodes.OK
            responseAs[List[CompilerError]].size should be >= (1)
          }

          Files.delete(invalidScript)
          val validFile = createValidScript(projPath)

          val compileRequest2 = HttpRequest(
            HttpMethods.POST,
            uri = "/moie/project/0/compileScript",
            entity = HttpEntity(MediaTypes.`application/json`, filePathFormat.write(FilePath(validFile.toString)).compactPrint))

          compileRequest2 ~> service.routes ~> check {
            Thread.sleep(5000)
            status shouldEqual StatusCodes.OK
            responseAs[List[CompilerError]] shouldBe empty
          }
        }

    "shutdown when calling /stop-server" in {
      Post("/moie/stop-server") ~> service.routes ~> check {
        status shouldEqual StatusCodes.Accepted
      }
      try {
        Await.ready(service.actorSystem.whenTerminated, 5 seconds)
      } catch {
        case _: TimeoutException => fail("ActorSystem didn't terminated as expected")
      }
    }
  }
}
