import sbt._

object Dependencies {

  private def fromGithub(url:String) = RootProject(uri(url))

  private val akkaVersion = "2.4.6"
  private val akkaGroup = "com.typesafe.akka"
  val akka = Seq(
    akkaGroup %% "akka-http-core" % akkaVersion,
    akkaGroup %% "akka-http-experimental" % akkaVersion,
    akkaGroup %% "akka-http-spray-json-experimental" % akkaVersion,
    akkaGroup %% "akka-http-testkit" % akkaVersion,
    akkaGroup %% "akka-testkit" % akkaVersion,
    akkaGroup %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.3"
  )

  private val scalaUtilsVersion = "1.0.4"
  private val scalaUtilsGroup = "org.scala-lang.modules"
  val scalaUtils = Seq(
     scalaUtilsGroup %% "scala-parser-combinators" % scalaUtilsVersion
  )

  val testLib = Seq(
      "org.scalactic" %% "scalactic" % "2.2.6",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )

  val ewsProject = fromGithub("git://github.com/njustus/EnhancedWatchService.git")

  val usedDependencies = akka ++ scalaUtils ++ testLib
}
