package de.thm.moie.declaration

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import akka.pattern.pipe
import de.thm.moie.position.FileWithLine
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.concurrent.Future
import scala.io.Source
import java.nio.file.{Path, Paths}

import omc.corba.ScriptingHelper
import de.thm.moie.Global

/*+ An Actor which finds the source (file) of a `className`. */
class JumpToProvider(jumpLike:JumpToLike)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import context.dispatcher

  override def handleMsg: Receive = {
    case DeclarationRequest(className) =>
      Future {
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
      } pipeTo sender
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
}
