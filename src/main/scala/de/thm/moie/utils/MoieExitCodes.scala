/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.utils

object MoieExitCodes {
  val CONFIG_ERROR = 2
  val UNMODIFIED_CONFIG = 1

  def waitAndExit(exitCode:Int): Unit = {
    Thread.sleep(1000)
    System.exit(exitCode)
  }
}
