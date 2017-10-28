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

import de.thm.mope.config.{Constants,CliConf, ConfigProvider}
import de.thm.mope.server._
import RecentFilesActor._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config

object MoPE
    extends MopeSetup {

  def main(args:Array[String]) = {
    val module = new MopeModule {
      override lazy val config: Config = new ConfigProvider(new CliConf(args.seq), Constants.configFile).config
      override implicit lazy val actorSystem:ActorSystem = ActorSystem("moie-system", config)
      override implicit lazy val mat:ActorMaterializer = ActorMaterializer()
    }
    module.server.start()
  }
}
