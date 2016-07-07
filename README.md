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

# Using an IDE
Because Mo|E uses several subprojects from different repositories it's not so easy to
setup an IDE.

## IntelliJ
Compile the subprojects with ```gradle compileJava```.
Add the subprojects as module dependencies.
Depend on the class-files in ```build/classes/main```.

## Eclipse
Import each subproject into the workspace and add these as project
dependencies for this project.

# REST-API
A documentation for the REST-API can be found in
``` doc/rest-api/ ```.
The documentation is a latex-file called ``` rest-api.tex ``` which can be translated into a pdf with ```make```.

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
