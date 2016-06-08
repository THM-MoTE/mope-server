/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

case class CompilerError(file:String,
                        start: FilePosition,
                        end:FilePosition,
                        message:String)
