/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
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

    if(!Files.isDirectory(realPath))
      errors += s"$realPath isn't a directory"

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
