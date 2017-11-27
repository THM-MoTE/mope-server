package de.thm.mope.lsp.messages

case class Diagnostic(range:Range,
                      severity:Int,
                      source:String,
                      message:String)
object Diagnostic {
  /**
    * Reports an error.
    */
  val Error = 1
  /**
    * Reports a warning.
    */
  val Warning = 2
  /**
    * Reports an information.
    */
  val Information = 3
  /**
    * Reports a hint.
    */
  val Hint = 4
}
