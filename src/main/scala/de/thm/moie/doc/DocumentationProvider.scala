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

package de.thm.moie.doc

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import de.thm.moie.utils.actors.UnhandledReceiver

import scala.concurrent.Future

/*+ An Actor which returns the documentation (as DocInfo) of a given `className`. */
class DocumentationProvider(docLike: DocumentationLike)
  extends Actor
    with UnhandledReceiver
    with ActorLogging {

  import DocumentationProvider._
  import context.dispatcher

  override def receive: Receive = {
    case GetDocumentation(className) =>
      Future {
        val docOpt = docLike.getDocumentation(className)
        log.info(
            if(docOpt.isDefined) "got documentation for {}"
            else "no documentation for {}",
            className)
        docOpt
      } pipeTo sender
  }
}

object DocumentationProvider {
  case class GetDocumentation(className:String)
  case class GetClassComment(className:String)
}
