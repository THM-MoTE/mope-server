fork in run := true
connectInput in run := true
cancelable in Global := true

scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature"
)

//include ./conf in classpath
unmanagedResourceDirectories in Compile += baseDirectory.value / "conf"
unmanagedResourceDirectories in Compile += baseDirectory.value / "compile-scripts"

lazy val configDir = settingKey[File]("The config directory of mope")
lazy val cleanConfig = taskKey[Unit]("Cleans user's config directory of mope")

cleanConfig := IO.delete(configDir.value)

sourceGenerators in Compile += Def.task {
  val dir:File = (sourceManaged in Compile).value
  InfoGenerator.generateProjectInfo(dir, Seq(
    "name" -> (name in root).value,
    "version" -> (version in root).value,
    "organization" -> (organization in root).value,
    "copyright" -> "() 2016-2018 Nicola Justus"))
}.taskValue

lazy val root = Project(id = "moie-server", base = file(".")).
  settings(
    organization := "de.thm.mote",
    name := "MoPE-server",
    version := "0.6.8",
    scalaVersion := "2.12.8",
    javacOptions ++= Seq("-source", "1.8"),
    mainClass in Compile := Some("de.thm.mope.MoPE"),
    configDir := new File(System.getProperty("user.home")) / ".mope",
    resolvers += Dependencies.jitpack,
    libraryDependencies ++= Dependencies.usedDependencies,
    parallelExecution in Test := false,
    aggregate in Test := false
  ).
  dependsOn(Dependencies.ewsProject, Dependencies.recentlyProject)

mainClass in assembly := (mainClass in Compile).value
assemblyJarName in assembly := s"mope-server-${version.value}.jar"
test in assembly := {} //skip test's during packaging
