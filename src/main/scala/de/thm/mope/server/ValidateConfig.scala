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

import com.typesafe.config.Config
import de.thm.mope.compiler.CompilerFactory

import scala.sys.process._

trait ValidateConfig {
  type Errors = List[String]

  def validateConfig(config:Config): Errors = {
    val buffer = scala.collection.mutable.ArrayBuffer[String]()
    val compilerKey = "compiler"
    val compilerExecKey = "compilerExecutable"

    if(!config.hasPath(compilerKey)) { //check which compiler
      buffer += s"`$compilerKey` is undefined. Specify which compiler to use. For example: $compilerKey=omc"
    } else if(!CompilerFactory.compilerKeys.contains(config.getString(compilerKey))) {
      val possibilities = CompilerFactory.compilerKeys.mkString(", ")
      buffer +=s"`$compilerKey`: ${config.getString(compilerKey)} isn't a valid option. Options are: $possibilities"
    }

    if(!config.hasPath(compilerExecKey)) { //check executable
      buffer += s"`$compilerExecKey` is undefined. Specify where the compiler executable is located."+
      s"For example: $compilerExecKey=/usr/bin/omc"
    } else {
      val executable = config.getString(compilerExecKey)
      try {
        (executable + " --help").!!
      } catch {
        case ex:Exception => buffer += s"`$compilerExecKey`: $executable wasn't executable: " + ex.getMessage
      }
    }

    buffer.toList
  }
}
