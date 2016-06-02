package de.thm.moie.compiler

case class CompilerError(file:String,
                         line:Int,
                         column:Int,
                         message:String)
