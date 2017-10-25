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

package de.thm.mope.config

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

trait FallbackConfig {
  private val fallbackMap =
    Map("force-english" -> "false",
      "indexFiles" -> "true",
      "exitOnLastDisconnect" -> "false",
      "app.mode" -> "prod",
      "defaultAskTimeout" -> "20",
      "http.interface" -> "127.0.0.1",
      "http.port" -> "9001")

  val fallbackConfig = ConfigFactory.parseMap(fallbackMap.asJava)
}
