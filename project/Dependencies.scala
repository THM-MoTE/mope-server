import sbt._
import sbt.io.Path

object Dependencies {

  def fromGithub(url:String) = RootProject(uri(url))
  def fromFile(url:String) = RootProject(file(url))
  def omcApi(snapshot:Boolean=false) =
    if(snapshot) Seq(
      "org.antlr" % "antlr4-runtime" % "4.5.3",
      "org.apache.commons" % "commons-lang3" % "3.5",
      "org.zeromq" % "jeromq" % "0.4.3"
    )
    else Seq("de.thm.mni.mote" % "omc-java-api" % "0.3.5")

  val jitpack = "jitpack" at "https://jitpack.io"

  private val akkaVersion = "2.4.19"
  private val akkaHTTPVersion = "10.0.10"
  private val akkaGroup = "com.typesafe.akka"

  val logging = Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "org.slf4j" % "jul-to-slf4j" % "1.7.21"
  )

  val akka = Seq(
    akkaGroup %% "akka-http-core" % akkaHTTPVersion,
    akkaGroup %% "akka-http-spray-json" % akkaHTTPVersion,
    akkaGroup %% "akka-http-testkit" % akkaHTTPVersion % Test,
    akkaGroup %% "akka-testkit" % akkaVersion % Test,
    akkaGroup %% "akka-slf4j" % akkaVersion
  )

  val testLib = Seq(
      "org.scalatest" %% "scalatest" % "3.0.3" % Test
  )
  private val scalaUtilsVersion = "1.0.6"
  private val scalaUtilsGroup = "org.scala-lang.modules"

  val utils = omcApi(true) ++ Seq(
    scalaUtilsGroup %% "scala-parser-combinators" % scalaUtilsVersion,
    "org.rogach" %% "scallop" % "3.1.1"
  )


  val macWireVersion = "2.3.0"
  val depInjection = Seq(
    "com.softwaremill.macwire" %% "macros" % macWireVersion % Provided,
    "com.softwaremill.macwire" %% "macrosakka" % macWireVersion % Provided,
    "com.softwaremill.macwire" %% "util" % macWireVersion
  )

  val ewsProject = fromGithub("https://github.com/THM-MoTE/EnhancedWatchService.git")
  val recentlyProject = fromGithub("https://github.com/THM-MoTE/recently.git")

  val usedDependencies = (akka ++ logging ++ utils ++ testLib ++ depInjection)
}
