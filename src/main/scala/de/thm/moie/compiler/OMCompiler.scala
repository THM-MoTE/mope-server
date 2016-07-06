/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler
import java.nio.file.{Files, Path, StandardOpenOption}

import de.thm.moie.utils.MonadImplicits._
import de.thm.moie.utils.ResourceUtils._
import omc.corba.ScriptingHelper._
import omc.corba._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util._

class OMCompiler(compilerFlags:List[String], executableName:String, outputDir:Path) extends ModelicaCompiler {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val msgParser = new MsgParser()
  private val omc: OMCInterface = new OMCClient(executableName)

  try {
    omc.connect()
  } catch {
    case e:Exception => log.error("Couldn't initialize omc connection", e)
  }

  private val moScriptSwitch = "--showErrorMessages"
  private val tmpScriptName = "compile.mos"

  private val isPackageMo:Path => Boolean = _.endsWith("package.mo")

  def sortPathes(paths:List[Path]): List[Path] =
    paths.map(p => p.getParent -> p).sorted.map(_._2)

  override def compile(files: List[Path]): Seq[CompilerError] = {
    val pathes = files.map(_.toAbsolutePath.toString)
    files.headOption match {
      case Some(path) if files.exists(isPackageMo) =>
        //generate a script to compile
        createOutputDir(outputDir)
        val rootProjectFile = outputDir.getParent.resolve("package.mo")
        withOutputDir(outputDir) {
          //expect a package.mo in root-directory
          if(Files.exists(rootProjectFile)) {
            parseResult(omc.call("loadFile", asString(rootProjectFile)))
          } else List(CompilerError("Error",
            rootProjectFile.toString,
            FilePosition(0,0),
            FilePosition(0,0),
            s"Expected a root `package.mo`-file in ${rootProjectFile.getParent}"))
        }
      case Some(path) =>
        createOutputDir(outputDir)
        val res = omc.sendExpression(s"""cd(${asString(outputDir)})""")
        if(res.result.contains(outputDir.toString)) {
          loadAllFiles(files)
        } else {
          log.error("Couldn't change working directory for omc into {}", outputDir)
          Seq[CompilerError]()
        }
      case None => Seq[CompilerError]()
    }
  }

  private def loadAllFiles(files:List[Path]): Seq[CompilerError] = {
    val fileList = asStringArray(files.asJava)
    val expr = s"""loadFiles($fileList)"""
    val res = omc.sendExpression(expr)
    log.debug("loadFiles() returned {}", res)
    val errOpt:Option[String] = res.error
    errOpt.map(parseErrorMsg).getOrElse(Seq[CompilerError]())
  }

  override def compileScript(path:Path): Seq[CompilerError] = {
    compileScript(path, List(moScriptSwitch))
  }

  private def compileScript(path:Path, compilerFlags:List[String]): Seq[CompilerError] = {
    val startDir = path.getParent
    withOutputDir(startDir) {
      omc.sendExpression("clear()")
      val resScript = omc.sendExpression(s"""runScript(${asString(path)})""")
      log.debug("runScript returned {}", resScript.result)
      parseResult(resScript)
    }
  }

  private def parseResult(result:Result)  = {
    val errOpt:Option[String] = result.error
    errOpt.map(parseErrorMsg).getOrElse(parseErrorMsg(result.result))
  }

  def parseErrorMsg(msg:String): Seq[CompilerError] =
    msgParser.parse(msg) match {
      case Success(v) => v
      case Failure(ex) =>
        log.warn(s"Error while parsing compiler-output: ${ex.getMessage} from\n$msg")
        Seq[CompilerError]()
    }

  private def createOutputDir(path:Path): Unit = {
    if(!Files.exists(path))
      Files.createDirectory(path)
  }

  private def generateTmpScript(outputPath:Path, rootProjectFile:Path): Path = {
    val scriptPath = outputPath.resolve(tmpScriptName)
    tryR(Files.newBufferedWriter(
      scriptPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      bw =>
        val content =
          s"""loadFile(${asString(rootProjectFile)});
             |getErrorString();""".stripMargin
        bw.write(content)
        scriptPath
    }
  }

  private def withOutputDir(dir: Path)(f: => Seq[CompilerError]): Seq[CompilerError] = {
    val res = omc.sendExpression(s"""cd(${asString(dir)})""")
    if (res.result.contains(dir.toString)) {
      f
    } else {
      log.error("Couldn't change working directory for omc into {}", dir)
      Seq[CompilerError]()
    }
  }

  override def stop(): Unit = omc.disconnect()
}
