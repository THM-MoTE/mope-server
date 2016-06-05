/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.project

case class ProjectDescription(
           path:String,
           outputDirectory:String,
           compilerFlags: List[String])
