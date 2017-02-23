package de.thm.mope

package object suggestion {
  def sliceAtLast(str:String,separator:String):(String, String) = {
    val idx = str.lastIndexOf(separator)
    if(idx != -1)
      str.splitAt(idx)
    else (str, "")
  }

  def sliceAtLastDot(str:String): (String, String) = sliceAtLast(str, ".")
}
