/**
  * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
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

package de.thm.mope.compiler

import java.nio.file.{Files, Path, Paths}

import de.thm.mope.config.{Constants, ProjectConfig}
import de.thm.mope.doc.DocInfo
import de.thm.mope.position.FilePosition
import de.thm.mope.suggestion.Suggestion.Kind
import de.thm.mope.tree.{ModelicaProjectTree, TreeLike}
import de.thm.mope.utils.IOUtils
import de.thm.mope.utils.MonadImplicits._
import de.thm.mope.models.SimulationResult
import omc.corba.ScriptingHelper._
import omc.corba._
import omc.ZeroMQClient
import omc.ior.ZMQPortFileProvider
import omc.{ImportHandler, LoadLibraryException}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util._
import com.github.tototoshi.csv.CSVReader

class OMCompiler(projConfig: ProjectConfig) extends ModelicaCompiler {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val msgParser = new MsgParser()
  private val omc: OMCInterface = {
    val forceEnglish = projConfig.server.config.getBoolean("forceEnglish")
    if (forceEnglish) new ZeroMQClient(projConfig.server.compilerExecutable, Constants.usLocale, new ZMQPortFileProvider("mope_local"))
    else new ZeroMQClient(projConfig.server.compilerExecutable)
  }
  private val paramRegex = """input\s*([\w\d]+)\s*([\w\d]+)""".r

  val rootProjectFile = projConfig.rootDir.resolve("package.mo")

  val loaderOpt = {
    val importFile = projConfig.rootDir.resolve(ImportHandler.importFileName)
    log.info(if (Files.exists(importFile)) s"Load libraries from $importFile" else "No libraries given")
    if (Files.exists(importFile)) Some(new ImportHandler(projConfig.rootDir))
    else None
  }

  omc.connect()
  IOUtils.createDirectory(projConfig.outputDir)

  private def parseFiles[A](projectTree: TreeLike[Path])(fn: Seq[CompilerError] => A): A = {
    /** 1. load all external libraries, if they exist
      * 2. load all package.mo files
      * 3. load all non-package.mo files
      */

    def parseFileList(files: Seq[Path]): Seq[CompilerError] = {
      log.debug("parseFiles files {}", files)
      for {
        file <- files
        errors <- parseResult(omc.call("loadFile", convertPath(file)))
      } yield {
        log.info("parsing returned: {}", errors)
        errors
      }
    }

    withOutputDir(projConfig.outputDir) {
      val pckMoDirs = ModelicaProjectTree.packageMoDirectories(projectTree)
      val plainFiles = ModelicaProjectTree.singleFiles(projectTree, pckMoDirs)
      val pckMoFiles = pckMoDirs.map(_.resolve(ModelicaProjectTree.packageFilename))

      val libErrors =
        loaderOpt.map { loader =>
          try {
            loader.loadLibraries(omc)
            Nil
          } catch {
            case ex: LoadLibraryException =>
              log.warn("Coudln't load all libraries")
              //transform exception into compiler warning
              for (error <- ex.errors.asScala)
                yield CompilerError("Warning", "", FilePosition(0, 0), FilePosition(0, 0), error)
          }
        }.getOrElse(Nil)

      val parseErrors = libErrors ++ parseFileList(pckMoFiles) ++ parseFileList(plainFiles)
      fn(parseErrors)
    }
  }

  override def compile(projectTree: TreeLike[Path], openedFile: Path): Seq[CompilerError] = {
    /** 1. load all package.mo files
      * 2. load all non-package.mo files
      * 3. typecheck
      */
    parseFiles(projectTree) { parseErrors =>
      val modelNameOp: Option[String] = ScriptingHelper.getModelName(openedFile)
      modelNameOp.
        map(typecheckIfEmpty(parseErrors, _)).
        getOrElse(parseErrors)
    }
  }


  private def loadAllFiles(files: List[Path]): Seq[CompilerError] = {
    val fileList = asArray(files.map(convertPath).asJava)
    val expr = s"""loadFiles($fileList)"""
    val res = omc.sendExpression(expr)
    val errOpt: Option[String] = res.error
    errOpt.map(parseErrorMsg).getOrElse(Seq[CompilerError]())
  }

  override def compileScript(path: Path): Seq[CompilerError] = {
    val startDir = path.getParent
    omc.sendExpression("clear()")
    withOutputDir(startDir) {
      val resScript = omc.sendExpression(s"""runScript(${convertPath(path)})""")
      log.debug("runScript returned {}", resScript)
      parseResult(resScript)
    }
  }

  override def checkModel(projectTree: TreeLike[Path], path: Path): String = {
    parseFiles(projectTree) { _ =>
      val modelnameOpt: Option[String] = ScriptingHelper.getModelName(path)
      modelnameOpt.
        map(omc.checkModel(_)).
        map(killTrailingQuotes).
        getOrElse("")
    }
  }

  override def getClasses(className: String): Set[(String, Kind.Value)] = {
    val classNames = omc.getList("getClassNames", className,
      java.lang.Boolean.valueOf(false),
      java.lang.Boolean.valueOf(true)).asScala

    val xs = classNames.zip(getCompletionType(classNames)).toSet
    xs
  }

  override def getGlobalScope(): Set[(String, Kind.Value)] = {
    val classNames = omc.getList("getClassNames").asScala
    classNames.zip(getCompletionType(classNames)).toSet
  }

  override def getParameters(className: String): List[(String, Option[String])] = {
    if (omc.is_("Model", className) || omc.is_("Class", className)) {
      val xs = omc.getList("getParameterNames", className)
      xs.asScala.
        map(killTrailingQuotes).
        map(_ -> None).toList
    } else if (omc.is_("Function", className)) {
      val res = omc.call("list", className)
      if (res.error.isPresent())
        Nil
      else {
        val xs = getParametersFromFunction(res.result).map {
          case (name, tpe) => name -> Some(tpe)
        }
        xs
      }
    } else Nil
  }

  override def getClassDocumentation(className: String): Option[String] = {
    val res = omc.call("getClassComment", className)
    val comment = killTrailingQuotes(res.result)
    if (comment.isEmpty) None
    else Some(comment)
  }

  override def getDocumentation(className: String): Option[DocInfo] = {
    val res = omc.call("getDocumentationAnnotation", className)
    if (res.error.isPresent) None
    else {
      val lst = fromArray(res.result).asScala.map(killTrailingQuotes).toList
      lst match {
        case info :: rev :: header :: _ if info.nonEmpty =>
          val classes = getClasses(className).map(_._1)
          val classComments = classes.map(getClassDocumentation)
          val subcomponents =
            classes.
              zip(classComments).
              map { case (name, comment) => DocInfo.Subcomponent(name, comment) }
          Some(DocInfo(info, rev, header, subcomponents))
        case _ => None
      }
    }
  }

  override def simulate(modelName:String, arguments:Map[String,String]): Try[SimulationResult] = {
    val code = arguments
      .updated("outputFormat", "\"csv\"")
      .map { case (k,v) => s"$k=$v"} //now encode in modelica code
      .toList
    log.debug("simulating {} with flags: {}", modelName:Any, code:Any)
    val res = omc.call("simulate", (modelName :: code):_*)
    if(res.error.isPresent && res.error.get.startsWith("Error")) {
      //fail on errors; not on warnings that are inside the error proprety
      Failure(SimulationError(s"simulating failed with: ${res.error.get}"))
    } else {
      val str = res.result
      log.debug("simulation returned: {}", str)
      //extracts the path from the SimulationResult record
      //see: https://build.openmodelica.org/Documentation/OpenModelica.Scripting.simulate.html
      val pathOpt = str match {
        case OMCompiler.simulationResultFilePattern(pathStr) => Some(Paths.get(pathStr)) //path to result.csv file
        case _ => None
      }
      //extract an error message from the result record
      val errorOpt = str match {
        case OMCompiler.simulationErrorPattern(error) => Some(error)
        case _ => None
      }

      val pathTry: Try[Path] = pathOpt match {
        case Some(p) => Success(p)
        case None => Failure(SimulationError(errorOpt.getOrElse("the result string didn't contain a 'resultFile' property")))
      }

      pathTry.map { p =>
        val reader = CSVReader.open(p.toFile, Constants.encoding.name)
        //CSVReader generates a stream of maps, where each map corresponds to 1 line (String -> String) but we want
        //a map with lists as values: each variable has different values over time
        val map = reader.toStreamWithHeaders.foldLeft(Map.empty[String, List[String]]) {
          (acc:Map[String, List[String]], currentMap:Map[String,String]) => currentMap.foldLeft(acc) {
            case (acc, (k,v)) if acc.contains(k) => acc.updated(k, v::acc(k))
            case (acc, (k,v)) => acc.updated(k, v::Nil)
          }
        }
        reader.close()

        //save eventual warnings into result
        val warningOpt:Option[String] = res.error.filter(_.startsWith("Warning"))

        //finally convert stringified-numbers to Doubles & reverse the generated list because we prepend to it
        val variables = map.mapValues(_.map(_.toDouble).reverse)
        SimulationResult(modelName, variables, warningOpt)
      }
    }
  }

  private def getCompletionType(classNames: Seq[String]): Seq[Kind.Value] = {
    classNames.map { x =>
      if (omc.is_("Function", x)) Kind.Function
      else if (omc.is_("Package", x)) Kind.Package
      else if (omc.is_("Type", x)) Kind.Type
      else if (omc.is_("Model", x)) Kind.Model
      else Kind.Class
    }
  }

  override def getSrcFile(className: String): Option[String] = {
    val res = omc.call("getSourceFile", className)
    extractPath(res.result)
  }

  private def parseResult(result: Result) = {
    val errOpt: Option[String] = result.error
    errOpt.map(parseErrorMsg).getOrElse(parseErrorMsg(result.result))
  }

  private def typecheckIfEmpty(xs: Seq[CompilerError], model: String): Seq[CompilerError] = {
    log.debug(s"$model is instantiatable: [{}]", !notInstantiatable(model))
    if (xs.nonEmpty) xs
    else if (notInstantiatable(model)) Seq()
    else {
      val res = omc.checkAllModelsRecursive(model)
      parseErrorMsg(res)
    }
  }

  private def notInstantiatable(model: String): Boolean =
    omc.is_("Partial", model) || omc.is_("Function", model)

  def parseErrorMsg(msg: String): Seq[CompilerError] = {
    log.debug("parsing OM error: {}", msg)
    msgParser.parse(msg) match {
      case Success(v) => v
      case Failure(ex) =>
        log.warn(s"Error while parsing compiler-output: ${ex.getMessage} from\n$msg")
        Seq(CompilerError("Error",
          "",
          FilePosition(0, 0),
          FilePosition(0, 0),
          s"Couldn't understand compiler:\n$msg"))
    }
  }

  private def withOutputDir[A](dir: Path)(f: => A): A = {
    val res = omc.cd(dir)
    if (Paths.get(killTrailingQuotes(res.result)).toRealPath() equals dir.toRealPath()) {
      f
    } else {
      log.error("Couldn't change working directory for omc into {} got {}", dir: Any, res)
      throw new IllegalStateException("cd() error")
    }
  }

  private def getParametersFromFunction(modelicaExpr: String): List[(String, String)] = {
    paramRegex.findAllMatchIn(modelicaExpr).map { m =>
      m.group(2) -> m.group(1)
    }.toList
  }

  override def stop(): Unit = omc.disconnect()
}

object OMCompiler {
  private[OMCompiler] val simulationResultFilePattern = """resultFile\s+=\s+\"([\w\\\/\.\-\+\s]+)\"""".r.unanchored
  private[OMCompiler] val simulationErrorPattern = """messages\s+=\s+\"\s+(.*)\",""".r.unanchored
}
