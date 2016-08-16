/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import spray.json._
import DefaultJsonProtocol._
import de.thm.moie.server.FileWatchingActor
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
  private val rootDir = outputDir.getParent()
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
    val fileArgs = if(isLib) rootDir.toString else files.mkString(" ")
    val args = s"${classFlag.getOrElse("")} -file $fileArgs"
    val prog = s"$executableName -- $scriptFile $args"
    log.debug("compiling as {} - executing {}", if(isLib) "Library" else "Files", s"$executableName -- $scriptFile":Any)
    val stdout = prog.!!
    if(stdout.contains("Nothing to compile")) Seq[CompilerError]()
    else if(stdout.trim == "JVM started.") Seq[CompilerError]()
    else {
      val str = stdout.replace("JVM started.", "").trim
      val erg = str.parseJson.convertTo[Seq[CompilerError]]
      log.debug("parsed json is: {}", erg)
      erg
    }
  }

  override def getSrcFile(className:String): Option[String] = {
    //skip all modelica classes
    if(className.startsWith("Modelica")) None
    else {
      //try to find in the project files
      val filesAndModels = FileWatchingActor.
        getFiles(rootDir, FileWatchingActor.moFileFilter).
        flatMap { x =>
          asOption(ScriptingHelper.getModelName(x)).
            map(name => x -> name)
        }
      filesAndModels find(_._2 == className) map(_._1.toString)
    }
  }

  override def stop(): Unit = ()
}