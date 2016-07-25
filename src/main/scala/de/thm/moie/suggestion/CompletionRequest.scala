package de.thm.moie.suggestion

import de.thm.moie.position.FilePosition

case class CompletionRequest(file:String,
                             position:FilePosition,
                             word:String)
