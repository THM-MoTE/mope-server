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

package de.thm.moie.project

import java.nio.file.{Files, Paths}

/** A description for projects */
case class ProjectDescription(
           path:String,
           outputDirectory:String,
           buildScript:Option[String])

object ProjectDescription {
  type Errors = List[String]

  /** Checks the given description and returns found errors. */
  def validate(descr:ProjectDescription):Errors = {
    val realPath = Paths.get(descr.path)
    val scriptPathOpt = descr.buildScript.map(realPath.resolve)
    val errors = scala.collection.mutable.ArrayBuffer.empty[String]

    if(!Files.exists(realPath))
      errors += s"`$realPath` doesn't exist"
    else if(!Files.isDirectory(realPath))
      errors += s"`$realPath` isn't a directory"

    scriptPathOpt.
      filterNot { p =>
        Files.isRegularFile(p) &&
        p.getFileName.toString.endsWith(".mos")
      }.
      foreach { path =>
          errors += s"$path isn't a regular *.mos file!"
      }

    errors.toList
  }
}
