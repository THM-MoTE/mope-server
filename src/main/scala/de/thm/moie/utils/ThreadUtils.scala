/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package  de.thm.moie.utils

import java.util.concurrent.ThreadFactory
import java.util.concurrent.Executors

object ThreadUtils {

  def faileSafeRun(fn: => Unit): Unit =
    try { fn } catch {
      case e:Exception =>
    }

  def namedThreadFactory(threadName:String):ThreadFactory = new ThreadFactory() {
    val factory = Executors.defaultThreadFactory()
    override def newThread(r:Runnable): Thread = {
      val thread = factory.newThread(r)
      thread.setName(threadName+"-"+thread.getName)
      thread
    }
  }
}
