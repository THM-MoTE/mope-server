package  de.thm.moie.utils

object ThreadUtils {

  def faileSafeRun(fn: => Unit): Unit =
    try { fn } catch {
      case e:Exception =>
    }
}
