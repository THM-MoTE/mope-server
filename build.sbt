scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature"
)

//include ./conf in classpath
unmanagedResourceDirectories in Compile += baseDirectory.value / "conf"

lazy val configDir = settingKey[File]("The config directory of moie")

lazy val cleanConfig = taskKey[Unit]("Cleans user's config directory of moie")

configDir := new File(System.getProperty("user.home")) / ".moie"

cleanConfig := IO.delete(configDir.value)

lazy val root = (project in file(".")).
  settings(
    organization := "thm",
    name := "Mo|E-server",
    version := "0.1",
    scalaVersion := "2.11.8",
    javacOptions ++= Seq("-source", "1.8")
  )

mainClass in Compile := Some("de.thm.moie.MoIE")
mainClass in assembly := (mainClass in Compile).value
assemblyJarName in assembly := s"${name.value}-${version.value}.jar"
test in assembly := {} //skip test's during packaging

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-core" % "2.4.6",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.6",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.6",
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
  )
