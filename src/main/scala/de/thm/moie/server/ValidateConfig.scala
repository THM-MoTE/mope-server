/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.server

import com.typesafe.config.Config
import scala.sys.process._

trait ValidateConfig {
  type Errors = List[String]

  def validateConfig(config:Config): Errors = {
    val buffer = scala.collection.mutable.ArrayBuffer[String]()
    val compilerKey = "compiler"
    val compilerExecKey = "compiler-executable"

    if(!config.hasPath(compilerKey)) { //check which compiler
      buffer += s"`$compilerKey` is undefined. Specify which compiler to use. For example: $compilerKey=omc"
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
