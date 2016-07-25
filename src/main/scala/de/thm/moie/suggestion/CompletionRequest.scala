package de.thm.moie.suggestion

import de.thm.moie.compiler.FilePosition

case class CompletionRequest(file:String,
                             position:FilePosition,
                             word:String)
