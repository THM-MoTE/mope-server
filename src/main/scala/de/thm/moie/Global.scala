/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie

import java.net.URL
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths}

import scala.io.Source

import de.thm.moie.compiler.ModelicaCompiler
import de.thm.moie.config.{Config, ConfigLoader}

object Global {

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
    "omc" -> classOf[de.thm.moie.compiler.OMCompiler]
  )

  /** Check if path exist; if not create it */
  private def withCheckConfigDirectory[A](fn: Path => A): A = {
    if(Files.notExists(configDirPath))
      Files.createDirectory(configDirPath)
    fn(configDirPath)
  }

  /** Copies the file from classpath to filePath if filePath doesn't exist */
  private def copyIfNotExist(filePath:Path, filename:String): Unit = {
    if(Files.notExists(filePath)) {
      val is = getClass.getResourceAsStream("/"+filename)
      Files.copy(is, filePath)
    }
  }

  /** Returns the absolute config-url from relativePath */
  private def getConfigFile(relativePath:String):URL =
    withCheckConfigDirectory { configPath =>
      val filePath = configPath.resolve(relativePath)
      copyIfNotExist(filePath, relativePath)
      filePath.toUri.toURL
    }

  def getCompilerClass: Class[ModelicaCompiler] = {
    val compilerKey = config.getString("compiler").
      getOrElse(throw new IllegalStateException("Can't run without a defined compiler"))

    val compilerClazz = compilerMappings(compilerKey).asInstanceOf[Class[ModelicaCompiler]]
    compilerClazz
  }

  def getCompilerExecutable: String = {
    config.getString("compiler-executable").
      orElse(config.getString("compiler")).
      getOrElse(throw new IllegalStateException("Can't run without a defined compiler-executable"))
  }


  def readValuesFromResource(path:URL)(filter: String => Boolean): List[String] = {
    Source.fromURL(path, encoding.displayName).getLines.flatMap {
      case s:String if filter(s) => List(s)
      case a:Any => Nil
    }.toList
  }

  lazy val encoding = Charset.forName("UTF-8")
  lazy val configFileURL = getConfigFile("moie.conf")
  lazy val config: Config = new ConfigLoader(configFileURL)

  lazy val copyright = "(c) 2016 Nicola Justus"
  lazy val version = "0.1"
}
