/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler
import java.nio.file.{Files, Path}

import de.thm.moie.project.CompletionResponse.CompletionType
import de.thm.moie.server.NotFoundException
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

  val rootProjectFile = outputDir.getParent.resolve("package.mo")

  private val stdLibClasses = List(
    "ModelicaServices",
    "Complex",
    "Modelica")

  try {
    omc.connect()
  } catch {
    case e:Exception => log.error("Couldn't initialize omc connection", e)
  }

  private val isPackageMo:Path => Boolean = _.endsWith("package.mo")

  def sortPathes(paths:List[Path]): List[Path] =
    paths.map(p => p.getParent -> p).sorted.map(_._2)

  def setupProject[A](files: List[Path])(fn: Seq[CompilerError] => A):A = {
    createOutputDir(outputDir)
    if(files.exists(isPackageMo)) {
      withOutputDir(outputDir) {
        //expect a package.mo in root-directory
        if(Files.exists(rootProjectFile)) {
          val xs = parseResult(omc.call("loadFile", asString(rootProjectFile)))
          fn(xs)
        } else throw new NotFoundException(s"Expected a root `package.mo`-file in ${rootProjectFile.getParent}")
      }
    } else {
      withOutputDir(outputDir) {
        val xs = loadAllFiles(files)
        fn(xs)
      }
    }
  }

  override def compile(files: List[Path], openedFile:Path): Seq[CompilerError] = {
    files.headOption match {
      case Some(path) =>
        try {
          setupProject(files) { xs =>
            val modelnameOpt: Option[String] = ScriptingHelper.getModelName(openedFile)
            log.debug(s"modelname in {} {}", openedFile, modelnameOpt: Any)
            modelnameOpt.
              map(typecheckIfEmpty(xs, _)).
              getOrElse(xs)
          }
        } catch {
          case e:NotFoundException => List(CompilerError("Error",
            rootProjectFile.toString,
            FilePosition(0, 0),
            FilePosition(0, 0),
            e.msg))
        }
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
      log.debug("runScript returned {}", resScript)
      parseResult(resScript)
    }
  }


  override def checkModel(files:List[Path], path: Path): String = {
    setupProject(files) { _ =>
      val modelnameOpt:Option[String] = ScriptingHelper.getModelName(path)
      log.debug(s"modelname in {} {}", path, modelnameOpt:Any)
      modelnameOpt.
        map(omc.checkModel(_)).
        map(killTrailingHyphens).
        getOrElse("")
    }
  }

  override def getClasses(className: String): Set[(String, CompletionType.Value)] = {
    val classNames = omc.getList("getClassNames", className,
      java.lang.Boolean.valueOf(false),
      java.lang.Boolean.valueOf(true)).asScala

    val xs = classNames.zip(getCompletionType(classNames)).toSet
    log.debug("suggestions: {}", xs)
    xs
  }

  override def getGlobalScope(): Set[(String, CompletionType.Value)] = {
    val classNames = omc.getList("getClassNames").asScala
    classNames.zip(getCompletionType(classNames)).toSet
  }

  override def getParameters(className: String): List[(String, Option[String])] = {
    if(omc.is_("Model", className) || omc.is_("Class", className)) {
      val xs = omc.getList("getParameterNames", className)
      xs.asScala.
        map(killTrailingHyphens).
        map(_ -> None).toList
    } else Nil
  }

  override def getClassDocumentation(className:String): Option[String] = {
    val res = omc.call("getClassComment", className)
    val comment = killTrailingHyphens(res.result)
    if(comment.isEmpty) None
    else Some(comment)
  }

  private def getCompletionType(classNames:Seq[String]): Seq[CompletionType.Value] = {
    classNames.map { x =>
      if(omc.is_("Function", x)) CompletionType.Function
      else if(omc.is_("Package", x)) CompletionType.Package
      else if(omc.is_("Type", x)) CompletionType.Type
      else if(omc.is_("Model", x)) CompletionType.Model
      else CompletionType.Class
    }
  }

  private def parseResult(result:Result)  = {
    val errOpt:Option[String] = result.error
    errOpt.map(parseErrorMsg).getOrElse(parseErrorMsg(result.result))
  }

  private def typecheckIfEmpty(xs:Seq[CompilerError], model:String):Seq[CompilerError] =
    if(xs.nonEmpty) xs
    else typecheckModel(model)

  private def typecheckModel(model:String): Seq[CompilerError] = {
    val res = omc.checkAllModelsRecursive(model)
    log.debug("checkModel returned {}", res)
    parseErrorMsg(res)
  }

  private def typecheckModels(): Seq[CompilerError] = {
    @scala.annotation.tailrec
    def typecheckTillError(xs:List[String]): Seq[CompilerError] = xs match {
      case hd::tl =>
        val erg = omc.checkAllModelsRecursive(hd)
        log.debug("checkModel returned {}", erg)
        val errors = parseErrorMsg(erg)
        if(errors.nonEmpty) errors
        else typecheckTillError(tl)
      case Nil => Seq[CompilerError]()
    }

    val modelResult = omc.call("getClassNames", "qualified=true")
    log.debug("getClassNames returned {}", modelResult)
      //get all classes & filter stdLib
    val models = fromArray(modelResult.result).asScala.diff(stdLibClasses).toList
    typecheckTillError(models)
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

  private def withOutputDir[A](dir: Path)(f: => A): A = {
    val res = omc.sendExpression(s"""cd(${asString(dir)})""")
    if (res.result.contains(dir.toString)) {
      f
    } else {
      log.error("Couldn't change working directory for omc into {}", dir)
      throw new IllegalStateException("cd() error")
    }
  }

  override def stop(): Unit = omc.disconnect()
}
