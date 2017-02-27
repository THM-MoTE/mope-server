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

package de.thm.mope.declaration

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import akka.pattern.pipe
import de.thm.mope.suggestion.SrcFileInspector
import de.thm.mope.position.{FileWithLine, CursorPosition}
import de.thm.mope.utils.actors.UnhandledReceiver

import scala.concurrent.Future
import scala.io.Source
import java.nio.file.{Path, Paths}

import omc.corba.ScriptingHelper
import de.thm.mope.Global
import akka.stream._
import akka.stream.scaladsl._

/*+ An Actor which finds the source (file) of a `className`. */
class JumpToProvider(jumpLike:JumpToLike)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import context.dispatcher

  implicit val mat = ActorMaterializer(namePrefix = Some("declaration-stream"))

  override def receive: Receive = {
    case DeclarationRequest(cursorPos) =>
      val possibleVariables = findVariable(cursorPos)
      val possibleModel = Future {
        val className = cursorPos.word
        val lastPointIdx = className.lastIndexOf(".")
        val modelname = if(lastPointIdx != -1) className.substring(lastPointIdx+1) else className
        val fileOpt = for {
          filename <- jumpLike.getSrcFile(className)
          path = Paths.get(filename)
          lineNo <- lineNrOfModel(path, modelname)
        } yield FileWithLine(filename, lineNo)
        val fileInfo = fileOpt.map(f => s"${f.path}:${f.line}").getOrElse("undefined")
        log.debug("declaration of {} is {}", className, fileInfo:Any)
        fileOpt
      }

      (for {
        modelOpt <- possibleModel
        variableOpt <- possibleVariables
      } yield modelOpt orElse variableOpt) pipeTo sender
  }

  private def lineNrOfModel(file:Path, model:String): Option[Int] = {
    val src = Source.fromFile(file.toUri, Global.encoding.name)
    src.getLines.zipWithIndex.find {
      case (line, idx) =>
        val matcher = ScriptingHelper.modelPattern.matcher(line)
        if(matcher.find())
          matcher.group(1) == model
        else false
    }.map(_._2)
  }

  private def findVariable(cursorPos:CursorPosition): Future[Option[FileWithLine]] = {
    new SrcFileInspector(Paths.get(cursorPos.file))
      .localVariables(None)
      .filter(x => x.name == cursorPos.word)
      .map(x => FileWithLine(cursorPos.file, x.lineNo))
      .toMat(Sink.headOption)(Keep.right)
      .run()
  }
}
