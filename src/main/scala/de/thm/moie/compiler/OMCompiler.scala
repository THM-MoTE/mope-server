/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler
import java.nio.file.{Files, Path}

import de.thm.moie.Global
import de.thm.moie.doc.DocInfo
import de.thm.moie.position.FilePosition
import de.thm.moie.server.NotFoundException
import de.thm.moie.suggestion.CompletionResponse.CompletionType
import de.thm.moie.utils.MonadImplicits._
import omc.corba.ScriptingHelper._
import omc.corba._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util._

class OMCompiler(executableName:String, outputDir:Path) extends ModelicaCompiler {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val msgParser = new MsgParser()
  private val omc: OMCInterface = {
    val forceEnglish = Global.config.getBoolean("force-english").getOrElse(false)
    if(forceEnglish) new OMCClient(executableName, Global.usLocale)
    else new OMCClient(executableName)
  }
  private val paramRegex = """input\s*([\w\d]+)\s*([\w\d]+)""".r

  require(outputDir.getParent != null, s"${outputDir.toAbsolutePath} parent can't be null")
  val rootProjectFile = outputDir.getParent.resolve("package.mo")

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
      modelnameOpt.
        map(omc.checkModel(_)).
        map(killTrailingQuotes).
        getOrElse("")
    }
  }

  override def getClasses(className: String): Set[(String, CompletionType.Value)] = {
    val classNames = omc.getList("getClassNames", className,
      java.lang.Boolean.valueOf(false),
      java.lang.Boolean.valueOf(true)).asScala

    val xs = classNames.zip(getCompletionType(classNames)).toSet
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
        map(killTrailingQuotes).
        map(_ -> None).toList
    } else if(omc.is_("Function", className)) {
      val res = omc.call("list", className)
      if(res.error.isPresent())
        Nil
      else {
        val xs = getParametersFromFunction(res.result).map {
          case (name, tpe) => name -> Some(tpe)
        }
        xs
      }
    } else Nil
  }

  override def getClassDocumentation(className:String): Option[String] = {
    val res = omc.call("getClassComment", className)
    val comment = killTrailingQuotes(res.result)
    if(comment.isEmpty) None
    else Some(comment)
  }

  override def getDocumentation(className:String): Option[DocInfo] = {
    val res = omc.call("getDocumentationAnnotation", className)
    if(res.error.isPresent) None
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

  private def getCompletionType(classNames:Seq[String]): Seq[CompletionType.Value] = {
    classNames.map { x =>
      if(omc.is_("Function", x)) CompletionType.Function
      else if(omc.is_("Package", x)) CompletionType.Package
      else if(omc.is_("Type", x)) CompletionType.Type
      else if(omc.is_("Model", x)) CompletionType.Model
      else CompletionType.Class
    }
  }

  override def getSrcFile(className:String): Option[String] = {
    val classOpt:Option[String] = omc.getClassInformation(className)
    classOpt.flatMap(extractPath)
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
    parseErrorMsg(res)
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

  private def getParametersFromFunction(modelicaExpr: String): List[(String, String)] = {
    paramRegex.findAllMatchIn(modelicaExpr).map { m =>
      m.group(2) -> m.group(1)
    }.toList
  }

  override def stop(): Unit = omc.disconnect()
}
