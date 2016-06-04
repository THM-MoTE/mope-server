/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

case class CompilerError(file:String,
                         line:Int,
                         column:Int,
                         message:String)
