/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import spray.json._
import DefaultJsonProtocol._
import de.thm.moie.server.JsonSupport
import de.thm.moie.Global
import de.thm.moie.utils.MonadImplicits._
import java.nio.file.{Files, Path, Paths}
import org.slf4j.LoggerFactory
import omc.corba._
import scala.sys.process._
import scala.language.postfixOps

class JMCompiler(executableName:String, outputDir:Path)
  extends ModelicaCompiler
  with StubCompiler
  with JsonSupport {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val scriptFile = Global.withCheckConfigDirectory { configPath =>
    val scriptDir = configPath.resolve("scripts")
    if(Files.notExists(scriptDir))
      Files.createDirectory(scriptDir)

    val scriptPath = scriptDir.resolve("jmodelica_compile.py")
    Global.copyIfNotExist(scriptPath, "jmodelica_compile.py")
    Global.copyIfNotExist(scriptDir.resolve("compiler_error.py"), "compiler_error.py")
    scriptPath
  }

  override def compile(files:List[Path], openedFile:Path): Seq[CompilerError] = {
    val modelname:Option[String] = ScriptingHelper.getModelName(openedFile)
    if(files.exists(isPackageMo))
      compile(files, modelname, true)
    else
      compile(files, modelname, false)
  }

  private def compile(files:List[Path], modelName:Option[String], isLib:Boolean): Seq[CompilerError] = {
    val classFlag = modelName.map(x => s"-classname $x")
    val fileArgs = if(isLib) outputDir.getParent().toString else files.mkString(" ")
    val args = s"${classFlag.getOrElse("")} -file $fileArgs"
    val prog = s"$executableName -- $scriptFile $args"
    log.debug("compiling as {}", if(isLib) "Library" else "Files")
    log.debug("executing {}", s"$executableName -- $scriptFile")
    val stdout = prog.!!
    log.debug("stdout is: {}", stdout)
    if(stdout.contains("Nothing to compile")) Seq[CompilerError]()
    else if(stdout.trim == "JVM started.") Seq[CompilerError]()
    else {
      val str = stdout.replace("JVM started.", "").trim
      println("str:\n"+str)
      val erg = str.parseJson.convertTo[Seq[CompilerError]]
      log.debug("parsed json is: {}", erg)
      erg
    }
  }

  override def stop(): Unit = ()
}
