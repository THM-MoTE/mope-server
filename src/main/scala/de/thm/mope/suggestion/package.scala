package de.thm.mope

package object suggestion {
  def sliceAtLast(str:String,separator:String):(String, String) = {
    val idx = str.lastIndexOf(separator)
    if(idx != -1)
      (str.substring(0, idx), str.substring(idx+1))
    else (str, "")
  }

  def sliceAtLastDot(str:String): (String, String) = sliceAtLast(str, ".")
}
