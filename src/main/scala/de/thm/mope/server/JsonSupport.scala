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

package de.thm.mope.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import de.thm.mope.compiler.CompilerError
import de.thm.mope.declaration.DeclarationRequest
import de.thm.mope.doc.ClassComment
import de.thm.mope.position._
import de.thm.mope.project._
import de.thm.mope.lsp._
import de.thm.mope.lsp.messages._
import de.thm.mope.suggestion.Suggestion.Kind
import de.thm.mope.suggestion.{CompletionRequest, Suggestion, TypeOf, TypeRequest}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsObject, JsString, JsValue, JsonReader, JsonWriter, RootJsonFormat}

import scala.util.Try

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val projectDescriptionFormat: RootJsonFormat[ProjectDescription] = jsonFormat3(ProjectDescription.apply)
  implicit val filePositionFormat:RootJsonFormat[FilePosition] = jsonFormat2(FilePosition)
  implicit val compileErrorFormat:RootJsonFormat[CompilerError] = jsonFormat5(CompilerError)
  implicit val filePathFormat:RootJsonFormat[FilePath] = jsonFormat1(FilePath.apply)
  implicit val fileWithLineFormat:RootJsonFormat[FileWithLine] = jsonFormat2(FileWithLine)
  implicit val completionRequestFormat:RootJsonFormat[CompletionRequest] = jsonFormat3(CompletionRequest)
  implicit val completionTypeFormat = new RootJsonFormat[Kind.Value] {
    override def write(tpe:Kind.Value) = JsString(tpe.toString)
    override def read(value:JsValue) = value match {
      case JsString(str) => Kind.withName(str)
      case _ => throw new DeserializationException("CompletionType.Value expected")
    }
  }
  implicit val suggestionFormat:RootJsonFormat[Suggestion] = jsonFormat5(Suggestion.apply)
  implicit val declarationRequestFormat:RootJsonFormat[DeclarationRequest] = jsonFormat1(DeclarationRequest)
  implicit val classCommentFormat:RootJsonFormat[ClassComment] = jsonFormat2(ClassComment)
  implicit val typeOfFormat:RootJsonFormat[TypeOf] = jsonFormat3(TypeOf)
  implicit val typeRequestFormat:RootJsonFormat[TypeRequest] = jsonFormat3(TypeRequest)
  implicit val cursorPosFormat:RootJsonFormat[CursorPosition] = jsonFormat3(CursorPosition)

  // ========= LSP messages
  implicit val unitReader = new JsonWriter[Unit] {
    override def write(obj: Unit) = JsObject()
  }
  implicit val rpcRequestFormat = jsonFormat4(RequestMessage)
  implicit val rpcNotificationFormat = jsonFormat3(NotificationMessage)
  implicit val rpcMessageFormat = new RootJsonFormat[RpcMessage] {
    override def read(json: JsValue):RpcMessage =
      Try[RpcMessage](rpcRequestFormat.read(json))
        .orElse(Try(rpcNotificationFormat.read(json)))
        .get

    override def write(obj: RpcMessage): JsValue = obj match {
      case x:RequestMessage => rpcRequestFormat.write(x)
      case x:NotificationMessage => rpcNotificationFormat.write(x)
    }
  }

  implicit val positionFormat = jsonFormat2(Position)
  implicit val rangeFormat = jsonFormat2(Range)
  implicit val locationFormat = jsonFormat2(Location)
  implicit val diagnosticFormat = jsonFormat5(Diagnostic.apply)
  implicit val respErrFormat = jsonFormat2(ResponseError.apply)
  implicit val respMsgFormat = jsonFormat4(ResponseMessage)
  implicit val initParamsFormat = jsonFormat5(InitializeParams)
  implicit val textDocIdentFormat = jsonFormat1(TextDocumentIdentifier)
  implicit val textDocumentPosFormat = jsonFormat2(TextDocumentPositionParams)
  implicit val didSaveNotifyFormat = jsonFormat1(DidSaveTextDocumentParams)
}
