scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature"
)

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
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.6"
  //"org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
  )
