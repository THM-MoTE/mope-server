import sbt._

object Dependencies {

  def fromGithub(url:String) = RootProject(uri(url))
  def fromFile(url:String) = RootProject(file(url))

  private val akkaVersion = "2.4.19"
  private val akkaHTTPVersion = "10.0.10"
  private val akkaGroup = "com.typesafe.akka"

  val configLib = "com.typesafe" % "config" % "1.3.0"

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

  private val scalaUtilsVersion = "1.0.6"
  private val scalaUtilsGroup = "org.scala-lang.modules"
  val scalaUtils = Seq(
     scalaUtilsGroup %% "scala-parser-combinators" % scalaUtilsVersion
  )

  val testLib = Seq(
      "org.scalatest" %% "scalatest" % "3.0.3" % Test
  )

  val moteLib = Seq(
    "de.thm.mni.mote" % "omc-java-api" % "0.3.4"
  )


  val macWireVersion = "2.3.0"
  val depInjection = Seq(
    "com.softwaremill.macwire" %% "macros" % macWireVersion % Provided,
    "com.softwaremill.macwire" %% "macrosakka" % macWireVersion % Provided,
    "com.softwaremill.macwire" %% "util" % macWireVersion
  )

  val ewsProject = fromGithub("https://github.com/THM-MoTE/EnhancedWatchService.git")
  val recentlyProject = fromGithub("https://github.com/THM-MoTE/recently.git")

  val usedDependencies = configLib +: (akka ++ logging ++ scalaUtils ++ testLib ++ moteLib ++ depInjection)
}
