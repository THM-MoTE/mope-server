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

import scala.sys.process._
import scala.language.postfixOps
import scala.concurrent.Future
import org.slf4j.LoggerFactory
import com.typesafe.config.Config
import scala.concurrent.ExecutionContext

class EnsembleHandler(config:Config, blockingExecutor: ExecutionContext) {
  private val log = LoggerFactory.getLogger(this.getClass)
  val execField = "mote.moveExecutable"
  private val helpRegex = """Move\s+\-\s+V(\d+\.\d+\.\d+)""".r
  val moveJar:Option[String] =
    if(config.hasPath(execField)) {
        //path known; check if it's MoVE
      val jar = config.getString(execField)
      val versionCmd = Seq("java", "-jar", jar, "-version")
      try {
        val stdout = versionCmd.!!
        helpRegex.findFirstIn(stdout) match {
          case Some(v) =>
            log.info("MoVE Version {} detected.", v)
            Some(jar)
          case None =>
            log.warn(createError(jar))
            None
        }
      } catch {
        case ex:Exception =>
          log.error(s"Couldn't execute {}. It seems like the given jar isn't MoVE!",versionCmd.mkString(" "):Any, ex)
          None
      }
    } else None

  def createError(jarPath:String): String =
    s"Given jar for MoVE ($jarPath) isn't Move! Opening a file in MoVE isn't possible."

  def openInMove(filepath:String): Either[String, Unit] = {
    moveJar.map { jar =>
      Future {
        val cmd = Seq("java", "-jar", jar, filepath)
        cmd.lineStream
      }(blockingExecutor)
      Right(())
    }.getOrElse(Left(s"MoVE unknown. Please specify an executable-jar in the configuration-file under $execField"))
  }
}
