/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie

import java.net.URL
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths}

import com.typesafe.config.{Config, ConfigFactory}
import de.thm.moie.compiler.ModelicaCompiler

import scala.io.Source

object Global extends FallbackConfig {

  object ApplicationMode extends Enumeration {
    val Development, Production = Value

    def parseString(str:String): Value = str.toLowerCase match {
      case "dev" | "development" => Development
      case "prod" | "production" => Production
      case _ => throw new IllegalArgumentException(s"Can't decide which mode $str represents")
    }
  }

  private val configDirectoryName = ".moie"
  private val homeDirPath = Paths.get(System.getProperty("user.home"))
  private val configDirPath = homeDirPath.resolve(configDirectoryName)

  private val compilerMappings:Map[String, Class[_]] = Map(
    "omc" -> classOf[de.thm.moie.compiler.OMCompiler],
    "jm" -> classOf[de.thm.moie.compiler.JMCompiler]
  )

  /** Check if path exist; if not create it */
  def withCheckConfigDirectory[A](fn: Path => A): A = {
    if(Files.notExists(configDirPath))
      Files.createDirectory(configDirPath)
    fn(configDirPath)
  }

  /** Copies the file from classpath to filePath if filePath doesn't exist */
  def copyIfNotExist(filePath:Path, filename:String): Boolean = {
    if(Files.notExists(filePath)) {
      val is = getClass.getResourceAsStream("/"+filename)
      Files.copy(is, filePath)
      false
    } else true
  }

  /** Returns the absolute config-url from relativePath */
  private def getConfigFile(relativePath:String):(URL, Boolean) =
    withCheckConfigDirectory { configPath =>
      val filePath = configPath.resolve(relativePath)
      val flag = copyIfNotExist(filePath, relativePath)
      (filePath.toUri.toURL, flag)
    }

  private def getCompilerClass: Class[ModelicaCompiler] = {
    val compilerKey = config.getString("compiler")
    compilerMappings(compilerKey).asInstanceOf[Class[ModelicaCompiler]]
  }

  private def getCompilerExecutable: String = {
    config.getString("compiler-executable")
  }

  def newCompilerInstance(outputDir:Path): ModelicaCompiler = {
    val executableString = Global.getCompilerExecutable
    val compilerClazz = Global.getCompilerClass
    val constructor = compilerClazz.getDeclaredConstructor(classOf[String], classOf[Path])
    constructor.newInstance(executableString, outputDir)
  }

  def readValuesFromResource(path:URL)(filter: String => Boolean): List[String] = {
    Source.fromURL(path, encoding.displayName).getLines.flatMap {
      case s:String if filter(s) => List(s)
      case a:Any => Nil
    }.toList
  }

  lazy val encoding = Charset.forName("UTF-8")
  lazy val (configFileURL, configDidExist) = getConfigFile("moie.conf")
  lazy val config: Config = ConfigFactory.parseURL(configFileURL).withFallback(fallbackConfig)
  lazy val usLocale = "en_US.UTF-8"

  lazy val copyright = "(c) 2016 Nicola Justus"
  lazy val version = "0.1"
}
