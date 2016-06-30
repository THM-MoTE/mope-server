/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler
import java.nio.file.{Files, Path, StandardOpenOption}

import de.thm.moie.utils.ProcessExitCodes
import de.thm.moie.utils.ProcessUtils._
import de.thm.moie.utils.ResourceUtils._
import org.slf4j.LoggerFactory

import scala.sys.process.Process
import scala.util._

class OMCompiler(compilerFlags:List[String], executableName:String, outputDir:String) extends ModelicaCompiler {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val msgParser = new MsgParser()

  private val moScriptSwitch = "--showErrorMessages"
  private val tmpScriptName = "compile.mos"

  private val isPackageMo:Path => Boolean = _.endsWith("package.mo")

  def sortPathes(paths:List[Path]): List[Path] =
    paths.map(p => p.getParent -> p).sorted.map(_._2)

  override def compile(files: List[Path]): Seq[CompilerError] = {
    val pathes = files.map(_.toAbsolutePath.toString)
    files.headOption match {
      case Some(path) if files.exists(isPackageMo) =>
        //TODO: HACK: use length of path to find shortest; shortest file is in root directory
        //FIX THIS BEFORE WITH BREADTH-FIRST ordering #18
        val rootProjectFile =
          files.map(_.toAbsolutePath).
          filter(isPackageMo).
          sortBy(p => p.toString.size).head
        //generate a script to compile
        val outputDir = createOutputDir(rootProjectFile.getParent)
        val scriptPath = generateTmpScript(outputDir, rootProjectFile)
        //exec omc & parse result
        val compilerExec = executableName :: compilerFlags ::: List(scriptPath.toAbsolutePath.toString)
        val cmd = Process(compilerExec, outputDir.toFile)
        val (status, stdout, _) = runCommand(cmd)
//        log.debug(s"run tmpScript with\n$stdout")
        parseErrorMsg(stdout)
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

  private def generateTmpScript(outputPath:Path, rootProjectFile:Path): Path = {
    val scriptPath = outputPath.resolve(tmpScriptName)
    tryR(Files.newBufferedWriter(
      scriptPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      bw =>
        val content =
          s"""loadFile("${rootProjectFile}");
             |getErrorString();""".stripMargin
        bw.write(content)
        log.debug(s"compile-script written to $scriptPath")
        scriptPath
    }
  }

}
