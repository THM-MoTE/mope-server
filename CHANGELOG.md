# Changelog of Mo|E-Server
## Version 0.6
**Alternative writing of Mo|E is now MoPE and mope!**

Majore changes:
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