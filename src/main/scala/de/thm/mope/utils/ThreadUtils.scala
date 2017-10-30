/**
  * Copyright (C) 2016,2017 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */


package de.thm.mope.utils

import java.util.concurrent.{Executors, ThreadFactory}

object ThreadUtils {

  def faileSafeRun(fn: => Unit): Unit =
    try {
      fn
    } catch {
      case e: Exception => e.printStackTrace()
    }

  def namedThreadFactory(threadName: String): ThreadFactory = new ThreadFactory() {
    val factory = Executors.defaultThreadFactory()

    override def newThread(r: Runnable): Thread = {
      val thread = factory.newThread(r)
      thread.setName(threadName + "-" + thread.getName)
      thread
    }
  }

  def runnable(f: () => Unit): java.lang.Runnable = new java.lang.Runnable() {
    override def run(): Unit = f()
  }
}
