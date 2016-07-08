# Mo|E - Modelica Integration for Editors
## Mo|E - Server
Mo|E brings IDE-features for Modelica into your favourite Editor.

This is the server-process for Mo|E.

# Subprojects
Mo|E uses the following subprojects:
- [omc-java-api](https://git.thm.de/njss90/omc-java-api) -
  Java-implementation of OMC's CORBA interface
- [EnhancedWatchService](https://github.com/njustus/EnhancedWatchService) -
  Wrapper for Java WatchServices

# Setup
Currently we are only supporting OpenModelica. Make sure that OpenModelica and OMC is
working. Please check if it's possible to run OMC from the terminal (```which omc```).

You can skip point 2 & 3 if you use our setup script located [here](https://git.thm.de/njss90/moie-server/blob/master/tools/setup.sh).

1. Install [sbt](http://www.scala-sbt.org/) in order to compile the projects.

2. Because Mo|E uses subprojects you need to clone all subprojects and this project into
one directory. This should look like this:
  ```
  parent/
    - moie-server/
    - omc-java-api/
    - EnhancedWatchService/
  ```
  __Site node:__ This procedure is only needed because we can't publish all our projects
  to github or maven central yet. Once we've checked the licensing issues the project's going
  to be open-source and available on github.

3. Get into the server-directory
  - Start sbt by typing ```sbt```
  - Compile moie-server by typing ```compile``` at the sbt prompt.
    (sbt will compile the subprojects too.)

4. Execute ```run``` to start the server. Mo|E will produce serveral logs during runtime.
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

After the first run Mo|E generates a configuration file located at ```~/.moie/moie.conf```. You can change it to suit your needs.

__DO NOT CHANGE THE ```akka { .. }```-SECTION!__
If you messed up your configuration just kill the whole ```~/.moie``` directory.

# Using an IDE
Because Mo|E uses several subprojects from different repositories it's not so easy to
setup an IDE.

## IntelliJ
Compile the subprojects with ```gradle compileJava```.
Add the subprojects as library dependencies.
Depend on the class-files in ```build/classes/main```.

## Eclipse
Import each subproject into the workspace and add these as project
dependencies for this project.

# REST-API
A documentation for the REST-API can be found in
``` doc/rest-api/ ```.
The documentation is a latex-file called ``` rest-api.tex ``` which
can be translated into a pdf with ``` make ```.

# Notes
  - The idea of an server-process and several editors that are comunicating with
  the server isn't new. This project is heavily inspired by the [ENSIME-project](http://ensime.github.io/)
  which is a convenient way to develop scala projects.
  If you are a Scala developer please give ENSIME a try.

  Our main goal is to provide a similar development environment for Modelica
  like ENSIME for Scala.

# Developer Informations
It's quite hard to find good, informative documentation for __OpenModelica__ and __OMC__'s behaviour in particular.
OMC doesn't always work as someone would expect. Especially for people coming from a Java, C background!
To save ours of headaches and lots of nightmares here are some useful links which are quite helpful.

- [OpenModelica System Documentation](https://openmodelica.org/svn/OpenModelica/tags/OPENMODELICA_1_9_0_BETA_4/doc/OpenModelicaSystem.pdf)
MUST READ :exclamation:

- [OpenModelica Scripting Reference](https://build.openmodelica.org/Documentation/OpenModelica.Scripting.html)
Helpful when writing Modelicascripts or communicating through CORBA

- [OpenModelica User's Guide](https://openmodelica.org/doc/OpenModelicaUsersGuide/latest/)
(not really helpfull while developing but for getting knowledge about the users!)
