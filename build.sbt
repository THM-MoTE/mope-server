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

lazy val root = Project(id = "moie-server", base = file(".")).
  settings(
    organization := "thm",
    name := "Mo|E-server",
    version := "0.1",
    scalaVersion := "2.11.8",
    javacOptions ++= Seq("-source", "1.8")
  ).
  dependsOn(Dependencies.ewsProject, Dependencies.corbaProject)

mainClass in Compile := Some("de.thm.moie.MoIE")
mainClass in assembly := (mainClass in Compile).value
assemblyJarName in assembly := s"moie-server-${version.value}.jar"
test in assembly := {} //skip test's during packaging

libraryDependencies ++= Dependencies.usedDependencies
