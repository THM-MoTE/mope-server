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

import java.nio.file.{Path, Paths}

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import akka.stream._
import akka.stream.scaladsl._
import de.thm.mope.position.{CursorPosition, FileWithLine}
import de.thm.mope.suggestion.SrcFileInspector
import de.thm.mope.utils.ResourceUtils
import de.thm.mope.utils.actors.UnhandledReceiver
import omc.corba.ScriptingHelper

import scala.concurrent.Future
import scala.io.Source

/*+ An Actor which finds the source (file) of a `className`. */
class JumpToProvider(
                      jumpLike: JumpToLike,
                      fileInspectorFactory: Path => SrcFileInspector)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import context.dispatcher

  implicit val mat = ActorMaterializer()

  override def receive: Receive = {
    case DeclarationRequest(cursorPos) =>
      val possibleVariables = findVariable(cursorPos)
      val possibleModel = findModel(cursorPos)
      (for {
        modelOpt <- possibleModel
        variableOpt <- possibleVariables
      } yield modelOpt orElse variableOpt) pipeTo sender
  }

  private def lineNrOfModel(file: Path, model: String): Option[Int] = {
    val src = Source.fromFile(file.toUri)(ResourceUtils.codec)
    src.getLines.zipWithIndex.find {
      case (line, idx) =>
        val matcher = ScriptingHelper.modelPattern.matcher(line)
        if (matcher.find())
          matcher.group(1) == model
        else false
    }
      .map(_._2)
      .map(_ + 1) //results in 0-based idx instead of line number
  }

  private def findModel(cursorPos: CursorPosition): Future[Option[FileWithLine]] = {
    Future {
      val className = cursorPos.word
      val lastPointIdx = className.lastIndexOf(".")
      val modelname = if (lastPointIdx != -1) className.substring(lastPointIdx + 1) else className
      val fileOpt = for {
        filename <- jumpLike.getSrcFile(className)
        path = Paths.get(filename)
        lineNo <- lineNrOfModel(path, modelname)
      } yield FileWithLine(filename, lineNo)
      val fileInfo = fileOpt.map(f => s"${f.path}:${f.line}").getOrElse("undefined")
      log.debug("declaration of {} is {}", className, fileInfo: Any)
      fileOpt
    }
  }

  private def findVariable(cursorPos: CursorPosition): Future[Option[FileWithLine]] = {
    fileInspectorFactory(Paths.get(cursorPos.file))
      .localVariables(None)
      .filter(x => x.name == cursorPos.word)
      .map(x => FileWithLine(cursorPos.file, x.lineNo))
      .toMat(Sink.headOption)(Keep.right)
      .run()
  }
}
