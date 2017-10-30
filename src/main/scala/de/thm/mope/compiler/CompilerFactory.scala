/**
  * Copyright (C) 2016,2017 Nicola Justus <nicola.justus@mni.thm.de>
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


package de.thm.mope.compiler

import de.thm.mope.config._
import de.thm.mope.project.ProjectDescription

class CompilerFactory(servConf: ServerConfig) {

  import CompilerFactory._

  val compilerKey = servConf.config.getString("compiler")

  require(availableCompilers.keys.exists(_ == compilerKey),
    s"The given compilerKey [$compilerKey] isn't a valid key!")

  def getCompilerClass: Class[_ <: ModelicaCompiler] = availableCompilers(compilerKey)

  def newCompiler(projectDescription: ProjectDescription): ModelicaCompiler = {
    val compilerClazz = getCompilerClass
    val constructor = compilerClazz.getDeclaredConstructor(classOf[ProjectConfig])
    constructor.newInstance(ProjectConfig(servConf, projectDescription))
  }
}

object CompilerFactory {
  private val availableCompilers: Map[String, Class[_ <: ModelicaCompiler]] =
    Map("omc" -> classOf[de.thm.mope.compiler.OMCompiler],
      "jm" -> classOf[de.thm.mope.compiler.JMCompiler])

  val compilerKeys = availableCompilers.keys.toSet
}
