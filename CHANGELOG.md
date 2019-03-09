# Changelog of Mo|E-Server

## Version 0.6.6
- add basic simulation route

## Version 0.6.5

- Use OMC's ZeroMQ interface
- Add shutdown hook for gracefull termination in non-interactive server-sessions
- dockerize the server

## Prerelease 0.6.4-zmq

- replace OMC's CORBA interface with ZeroMQ, so that mope still works with `java-11`.
  Java `11` will remove the CORBA APIs from the standard library.

## Version 0.6.3
- Update dependency versions:
  - scala 2.12
  - akka 2.4.19
  - akkaHTTP 10.0.10
  - sbt 1.0.2

- better auto completion based on `String#startsWith` and Levenshtein distance
- allow configuration of compiler path, port and used protocol through command line arguments
  - the configuration file doesn't need to be tweaked anymore before first usage
- configuration directory is now `~/.config/mope` instead of `~/.mope`
- fallback configuration is now inside the resources and called `fallback.conf`
- Use dependency injection framework for wiring the app
- Use Ctrl+D to terminate server app
- several code refactorings & bug fixes
- first preparation for inclusion of the Language Server Protocol

- temporarily broke all tests!

## Version 0.6.2
- protocol documentation now in AsciiDoc
- internal project representation now as *(filesystem) tree*; not as file list
- adds opening a file in MoVE via `POST mope/ensemble/move`
- go to declaration now features local variables & go to declaration uses a new
  protocol specification: `POST mope/project/:id/declaration`
- suggestions are additionally matched using the levenshtein distance
- adds retrieving recently used projects through `GET mope/recent-files`

### Bugfixes
- resolve home directory at runtime
- don't typecheck functions & partial models
- allow errors with OMC-specific paths containing `\/` path delimiters


## Version 0.6
**Alternative writing of Mo|E is now MoPE and mope!**

Major changes:
- Config-Keys are all in Camel case
- Remove unused `filewatcher-polling-timeout` from mope.conf
- Programinformations (like name & version) now generated from within sbt
- Retrieving a DocumentationComment removed!

Renamings:
  - package `moie` => `mope`
  - `CompletionResponse` => `Suggestion`
  - `CompletionResponse.ComlpetionType` => `Kind`
  - `MoieExitcodes` => `MopeExitCodes`
  - `rest-api` => `protocol`

## Version 0.5
  - It's possible to use the JModelica compiler instead of OpenModelica
  - OpenModelica's compiler messages are forced to be in english
  - Own config loader (de.thm.moie.config) replaced with typesafe's config library
  - Improved validation of `moie.conf` and ProjectDescription
  - Declaration of a class/model now contains the linenumber
  - Add /typeOf to retrieve the type and Classcomment of a given variable
  - Add /comment to retrieve the Classcomment of a given class/model
  - local variables included in code suggestions
  - class/model properties included in code suggestions
  - Add type of suggested variable to suggestions
  - Update styles of documentation templates
  - Test updates:
    - Tests are now sequential
    - Tests are faster
  - Bugfixes:
    - Crash due pathes with \ on Windows systems

## Version 0.4
  This is the first useful version with a
  quite stable HTTP/REST API.
  The API documentation is up-to-date.

  Features implemented:
  - Compiling projects; either couple of files or
    projects defined through `package.mo` files

  - Execute scripts and check their errors; either
    the currently open file or a default script

  - Check models for their number and equations;
    uses OMC's checkModel() scripting function

  - Get suggestions for a symbol. Also known as `code completion`.
    It's possible to retrieve possible suggestions/completions
    for given classnames. Currently only full-qualified classnames
    are supported.

  - Find the source of a classname. Better known as
    `go to declaration`.

  - Display documentation annotations. The user-written
    documentation is embedded inside a HTML-template and
    supplied through Akka HTTP.

Major implementation changes:
  - Errors are handled in ErrorHandling
  - Multiple projects are managed by ProjectsManager
  - Projects are managed by 1 ProjectManager
  - The ProjectManager starts several sub-actors for
    handling specific tasks
