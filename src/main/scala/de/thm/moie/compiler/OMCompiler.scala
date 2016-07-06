/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler
import java.nio.file.{Files, Path, StandardOpenOption}

import de.thm.moie.utils.MonadImplicits._
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

  private val isPackageMo:Path => Boolean = _.endsWith("package.mo")

  def sortPathes(paths:List[Path]): List[Path] =
    paths.map(p => p.getParent -> p).sorted.map(_._2)

  override def compile(files: List[Path]): Seq[CompilerError] = {
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
        withOutputDir(outputDir)(loadAllFiles(files))
      case None => Seq[CompilerError]()
    }
  }

  private def loadAllFiles(files:List[Path]): Seq[CompilerError] = {
    val fileList = asStringArray(files.asJava)
    val expr = s"""loadFiles($fileList)"""
    val res = omc.sendExpression(expr)
    val errOpt:Option[String] = res.error
    errOpt.map(parseErrorMsg).getOrElse(Seq[CompilerError]())
  }

  override def compileScript(path:Path): Seq[CompilerError] = {
    val startDir = path.getParent
    withOutputDir(startDir) {
      omc.sendExpression("clear()")
      val resScript = omc.sendExpression(s"""runScript(${asString(path)})""")
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
