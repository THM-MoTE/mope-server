/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler
import java.nio.file.{Files, Path}

import de.thm.moie.utils.ProcessExitCodes
import de.thm.moie.utils.ProcessUtils._

import scala.util._
import scala.sys.process.Process

import org.slf4j.LoggerFactory

class OMCompiler(compilerFlags:List[String], executableName:String, outputDir:String) extends ModelicaCompiler {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val msgParser = new MsgParser()

  private val moScriptSwitch = "--showErrorMessages"

  override def compile(files: List[Path]): Seq[CompilerError] = {
    val pathes = files.map(_.toAbsolutePath.toString)
    files.headOption match {
      case Some(path) =>
        val outputDir = createOutputDir(path.getParent)
        val compilerExec = executableName :: (compilerFlags ::: pathes)
        val cmd = Process(compilerExec, outputDir.toFile)
        val (status, stdout, _) = runCommand(cmd)
        if(status != ProcessExitCodes.SUCCESSFULL) parseErrorMsg(stdout)
        else Seq[CompilerError]()
      case None => Seq[CompilerError]()
    }
  }

  override def compileScript(path:Path): Seq[CompilerError] = {
    val startDir = path.getParent
    val compilerExec = List(executableName, moScriptSwitch, path.toAbsolutePath.toString)
    val cmd = Process(compilerExec, startDir.toFile)
    val (_, _, stderr) = runCommand(cmd)
    parseErrorMsg(stderr)
  }

  def parseErrorMsg(msg:String): Seq[CompilerError] =
    msgParser.parse(msg) match {
      case Success(v) => v
      case Failure(ex) =>
        log.warn(s"Error while parsing compiler-output: {} from\n$msg", ex.getMessage)
        Seq[CompilerError]()
    }

  private def createOutputDir(path:Path): Path = {
    val outputPath = path.resolve(outputDir)
    if(!Files.exists(outputPath))
      Files.createDirectory(outputPath)
    outputPath
  }
}
