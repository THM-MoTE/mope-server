/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package  de.thm.moie.utils

object ThreadUtils {

  def faileSafeRun(fn: => Unit): Unit =
    try { fn } catch {
      case e:Exception =>
    }
}
