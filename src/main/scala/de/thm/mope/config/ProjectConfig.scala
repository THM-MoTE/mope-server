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

import java.nio.file.{Path, Paths}

import de.thm.mope.project.ProjectDescription

/** Settings for a specific project, including Server's settings. */
case class ProjectConfig(
                          server: ServerConfig,
                          project: ProjectDescription) {
  val rootDir: Path = Paths.get(project.path)
  val outputDir: Path = rootDir.resolve(project.outputDirectory)
}
