import sbt._

object Dependencies {

  def fromGithub(url:String) = RootProject(uri(url))
  def fromFile(url:String) = RootProject(file(url))

  private val akkaVersion = "2.4.6"
  private val akkaGroup = "com.typesafe.akka"

  val configLib = "com.typesafe" % "config" % "1.3.0"

  val logging = Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "org.slf4j" % "jul-to-slf4j" % "1.7.21"
  )

  val akka = Seq(
    akkaGroup %% "akka-http-core" % akkaVersion,
    akkaGroup %% "akka-http-experimental" % akkaVersion,
    akkaGroup %% "akka-http-spray-json-experimental" % akkaVersion,
    akkaGroup %% "akka-http-testkit" % akkaVersion % Test,
    akkaGroup %% "akka-testkit" % akkaVersion % Test,
    akkaGroup %% "akka-slf4j" % akkaVersion
  )

  private val scalaUtilsVersion = "1.0.4"
  private val scalaUtilsGroup = "org.scala-lang.modules"
  val scalaUtils = Seq(
     scalaUtilsGroup %% "scala-parser-combinators" % scalaUtilsVersion
  )

  val testLib = Seq(
      "org.scalatest" %% "scalatest" % "2.2.6" % Test
  )

  val ewsProject = fromFile("../EnhancedWatchService")
  val corbaProject = fromFile("../omc-java-api/")
  val recentlyProject = fromFile("../recently")

  val usedDependencies = configLib +: (akka ++ logging ++ scalaUtils ++ testLib)
}
