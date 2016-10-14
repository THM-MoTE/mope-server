scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature"
)

parallelExecution in Test := false

//include ./conf in classpath
unmanagedResourceDirectories in Compile += baseDirectory.value / "conf"
unmanagedResourceDirectories in Compile += baseDirectory.value / "compile-scripts"

lazy val configDir = settingKey[File]("The config directory of mope")

lazy val cleanConfig = taskKey[Unit]("Cleans user's config directory of mope")

configDir := new File(System.getProperty("user.home")) / ".mope"

cleanConfig := IO.delete(configDir.value)

sourceGenerators in Compile <+= Def.task {
  val dir:File = (sourceManaged in Compile).value
  InfoGenerator.generateProjectInfo(dir, Seq(
    "name" -> (name in root).value,
    "version" -> (version in root).value,
    "organization" -> (organization in root).value))
}

lazy val root = Project(id = "moie-server", base = file(".")).
  settings(
    organization := "thm",
    name := "MoPE-server",
    version := "0.5",
    scalaVersion := "2.11.8",
    javacOptions ++= Seq("-source", "1.8"),
    aggregate in Test := false
  ).
  dependsOn(Dependencies.ewsProject, Dependencies.corbaProject).
  aggregate(Dependencies.corbaProject)

mainClass in Compile := Some("de.thm.mope.MoPE")
mainClass in assembly := (mainClass in Compile).value
assemblyJarName in assembly := s"mope-server-${version.value}.jar"
test in assembly := {} //skip test's during packaging

libraryDependencies ++= Dependencies.usedDependencies
