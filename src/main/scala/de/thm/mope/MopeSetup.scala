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

package de.thm.mope

import ch.qos.logback.classic.{Level, Logger}
import de.thm.mope.config.ApplicationMode
import org.slf4j.LoggerFactory

/** Application wide initializations */
trait MopeSetup {

 def configureLogging(applicationMode:ApplicationMode.Value):Unit = {
  /* ================ SLF4J & LOGBACK */
  /* Route java.util.logging into slf4j */
  // remove existing handlers attached to j.u.l root logger
  org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger()
  // add SLF4JBridgeHandler to j.u.l's root logger
  org.slf4j.bridge.SLF4JBridgeHandler.install()

  /* ================ Application-wide max log level */
  val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
  applicationMode match {
   case ApplicationMode.Development => rootLogger.setLevel(Level.DEBUG)
   case ApplicationMode.Production => rootLogger.setLevel(Level.INFO)
  }
 }
}
