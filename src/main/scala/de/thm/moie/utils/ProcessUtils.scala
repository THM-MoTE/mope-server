package de.thm.moie.utils

import scala.sys.process._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.io.ByteArrayOutputStream
import java.io.PrintWriter


object ProcessUtils {
  def runCommand(cmd: Seq[String]): (Int, String, String) = {
    val stdoutStream = new ByteArrayOutputStream
    val stderrStream = new ByteArrayOutputStream
    val stdoutWriter = new PrintWriter(stdoutStream)
    val stderrWriter = new PrintWriter(stderrStream)
    val exitValue = cmd.!(ProcessLogger(stdoutWriter.println, stderrWriter.println))
    stdoutWriter.close()
    stderrWriter.close()
    (exitValue, stdoutStream.toString, stderrStream.toString)
  }

  def runCommandAsync(cmd:Seq[String])(
    implicit exec:ExecutionContext): Future[(Int, String, String)] =
      Future(runCommand(cmd))
}
