# MoPE - Modelica Integration for Editors
MoPE brings IDE-features for Modelica into your favourite Editor. Features are
documented in the [Feature Overview](doc/features.md).

## MoPE - Server
This is the server-process for MoPE.

# Subprojects
MoPE uses the following subprojects:
- [omc-java-api](https://git.thm.de/njss90/omc-java-api) -
  Java-implementation of OMC's CORBA interface
- [EnhancedWatchService](https://github.com/njustus/EnhancedWatchService) -
  Wrapper for Java WatchServices

# Setup
Currently we are only supporting OpenModelica. Make sure that OpenModelica and OMC is
working. Please check if it's possible to run OMC from the terminal (```which omc```).

Make sure you have a `$JAVA_HOME` environment variable. On Linux-Systems you can by adding the
following line to your `~/.bashrc`-file:
```sh
export JAVA_HOME=<path-to-java>
```

You can skip point 2 & 3 if you use our setup script located [here](https://git.thm.de/njss90/moie-server/blob/master/tools/setup.sh).

1. Install [sbt](http://www.scala-sbt.org/) in order to compile the projects.

2. Because MoPE uses subprojects you need to clone all subprojects and this project into
one directory. This should look like this:
  ```
  parent/
    - moie-server/
    - omc-java-api/
    - EnhancedWatchService/
    - moie-atom-plugin/
  ```
  __Site node:__ This procedure is only needed because we can't publish all our projects
  to github or maven central yet. Once we've checked the licensing issues the project's going
  to be open-source and available on github.

3. Get into the server-directory
  - Start sbt by typing ```sbt```
  - Compile moie-server by typing ```compile``` at the sbt prompt.
    (sbt will compile the subprojects too.)

4. Execute ```run``` to start the server. MoPE will produce several logs during runtime.
  Especially the starting log should look similar to this:
```
[info] Running de.thm.moie.MoIE
2016-07-08 19:26:50,077 [INFO ] a.e.s.Slf4jLogger [] - Slf4jLogger started
2016-07-08 19:26:50,077 [INFO ] a.e.s.Slf4jLogger [] - Slf4jLogger started
2016-07-08 19:26:50,828 [INFO ] d.t.m.s.Server [moie-server] - Server running at localhost:9001
2016-07-08 19:26:50,838 [INFO ] d.t.m.s.Server [moie-server] - Press Enter to interrupt
```
  The log tells you on which server (localhost) and port (9001) the server is listening.

5. Set the interface and port in the Atom-plugin in order to connect to the server.

After the first run MoPE generates a configuration file located at ```~/.mope/mope.conf```. You can change it to suit your needs.

__DO NOT CHANGE THE ```akka { .. }```-SECTION!__
If you messed up your configuration just kill the whole ```~/.moie``` directory.

# Creating a executable-jar
You can create an executable-jar using the `sbt assembly` command. This command packages
everything - including all dependencies - in one jar which is executable using `java -jar <jar>`.


# Using an IDE

## IntelliJ
Its possible to import this project as an sbt project.

If you are using Mac OS X make sure that IntelliJ "sees" the
```$JAVA_HOME``` variable. OS X doesn't publish environment
variables to GUI applications which are set inside
```.bash_profile```, ```.bashrc```, ...You have to set
the ```$JAVA_HOME``` variable inside
the file ```/etc/launchd.conf```. This file is visible to GUI applications.
Further information for this strange behaviour of Mac OS X are available here:
http://stackoverflow.com/questions/135688/setting-environment-variables-in-os-x

## Eclipse
Import each subproject into the workspace and add these as project
dependencies for this project.

# `mope-project.json` example
In order to connect to the server you need a `mope-project.json` file inside
of the root directory of your project. The file should look similar to this:
```json
{
	"path": "/home/user/dev/my-project",
	"outputDirectory": "target"
}
```
- `path` should be an absolute path to the project root directory.
- `outputDirectory` is a relative path inside of the projects
  root directory which is used as output directory

See the REST-API documentation for more details.

# REST-API / Protocol documentation
A documentation for the REST-API can be found in
``` doc/protocol/ ```.
The documentation is a latex-file called ``` protocol.tex ``` which
can be translated into a pdf with ``` make ```.

# Notes
  - The idea of an server-process and several editors that are communicating with
  the server isn't new. This project is heavily inspired by the [ENSIME-project](http://ensime.github.io/)
  which is a convenient way to develop scala projects.
  If you are a Scala developer please give ENSIME a try.

  Our main goal is to provide a similar development environment for Modelica
  like ENSIME for Scala.

# Developer Information's
Side notes about our tests:
- Currently our tests are targeted at OMC and are going to
  fail if you are compiling using JModelica

Some developer information about OpenModelica and OMC which are quite helpful:

- [OpenModelica System Documentation](https://openmodelica.org/svn/OpenModelica/tags/OPENMODELICA_1_9_0_BETA_4/doc/OpenModelicaSystem.pdf)

- [OpenModelica Scripting Reference](https://build.openmodelica.org/Documentation/OpenModelica.Scripting.html)
Helpful when writing Modelicascripts or communicating through CORBA

- [OpenModelica User's Guide](https://openmodelica.org/doc/OpenModelicaUsersGuide/latest/)
