## Einleitung
  - Was, Wofür ist Modelica?
  - Im Kern textbasiert => Editierung in Texteditor, IDE etc.
  - ???

  - Editierung in Editoren mit optionaler Fehleranzeige
  - Alternativen auf dem Open Source Markt: OMEdit, MDT
  - OMEdit nur begrenzte Unterstützung bei Fehlern
    - Anzeige der Fehler in einer Box
    - Typprüfung muss manuell angestossen werden über `check Model`
    - keine Hinweise im Editor
    - Fehlerhafte Dateien nicht speicherbar
    - Langsam bei großen Projekten
    - automatische Codeformatierung von Kommentaren
    - Entfernung von unnötigem Whitespace
    - extrem kompliziert zu installieren auf nicht-Windows Systemen
  - MDT
    - basiert auf Eclipse
    - besitzt integrierten Debugger
    - genau wie OMEdit langsam bei großen Projekten
    - lange Ladezeiten
    - verbraucht konstant **viel Speicher** durch den Eclipse Unterbau
    - ???
  - OneModelica ? - Stand-alone Eclipse
  - Zugeschnitten auf OpenModelica Compiler, Nutzung eines Alternativen
    Compilers im bekannten Editor nicht möglich

  - effektivstes Arbeiten im favorisiertesten Texteditor des Endanwenders
    - Nutzung des lieblings Editors unmöglich!
  - Anwender möchten ein Plugin zur komfortablen Editierung im gewohnten
    Texteditor
  - Editoren teilweise nur begrenzt erweiterbar
    - Vim erweiterbar durch VimScript
    - Emacs erweiterbar durch Emacs Lisp
    - Sublime Text durch Python
    - Atom durch JS
  - Wenn Unterstützung mehrerer Texteditoren =>
    Implementierung der Features in vielen verschiedenen
    Sprachen => extrem hoher Aufwand
      - Sprachen unterschiedlich mächtig & unterschiedlich beliebt
        - Vimscript kann niemand
        - JS will niemand
  - deswegen Editor Plugin minimalistisch + externer Prozess mit
  den IDE Aufgaben (siehe auch Ensime als Vorbild)
2. bessere Alternative
  - minimales Plugin für Editoren, die mit einem separatem
    Prozess kommunizieren, der die komplexen Aufgaben übernimmt
  - separater Prozess in nebenläufig und mächtiger, plattformunabhängiger Sprache
  - Client-Server-Architektur zwischen Editor + separatem Prozess
  - Was mach ich & dafür benötige ich Aktoren .. die funktionieren so


## Grundlagen
- IDEs
- Client-/Server Systeme
- HTTP
- (Reaktive Systeme)
- Ensime Projekt
- CORBA
- (OpeModelica)
- (JModelica)

## Glossar
API, HTTP, JS, JSON, IDE, OM

## Konzeption
Noch keine technischen Details, wie etwa HTTP, JSON?
- C-/S Struktur, in der Texteditoren Rolle des aktiven Clienten übernehmen
- Server = Compile-Service

### Server
- Arbeitet nur bei Bedarf, ist passiver Kommunikationspartner
- Verwaltet Projekte, wobei 1 Projekt definiert ist als 1 Ordnerstruktur
  - weist diesen eindeutige IDs zu, wenn sich ein Client mit einem Projekt
    anmeldet
  - (Indiziert zugehörige Quelldateien --
      evtl. in Umsetzung? Grund ist Performance?)
- Stößt Kompiliervorgänge (evtl. als Subprozess) an und
  parst das Ergebnis (den Output) der verschiedenen Compiler
- Findet Quelldateien, die Deklaration von Typen
- Generiert Autovervollständigungen für gegebene Wörter anhand der Quelldateien
- Findet den Typ & Comment von Variablen
- Bettet HTML-Dokumentation in HTML ein und stellt sie zur Verfügung

### Client
- beliebige Texteditoren oder andere textdarstellende Programme
- bieten bereits umfangreiche Editierungsmöglichkeiten; Implementierung
  Editorfeatures unnötig
- melden sich einmalig an um mit Server zu kommunizieren
- kommunizieren **bei Bedarf/auf Anfrage** mit Server
  (z.B.: nach dem speichern => compilieren)
- stellen Serverantwort sinnvoll dar (Atom Zeilenmarkierung +
  Fehlerbeschreibung unten)
- Fragen nach Autovervollständigung für Wort unter/bevor dem Cursor
- Fragen nach Quelldatei eines Typs, durch Nutzer per Strg + Click ausgeführt

### Protokoll
- Schaubild zur Kommunikation (siehe Präsentation)
- HTTP in Verbindung mit JSON
- Zustandsbehaftet durch Angabe der Projekt-ID (project/**20**/*TASK*)


## Literatur
- Benutzbarkeit von Modelica
- Modelica in der Lehre
- Benutzbarkeit Modelicatools
- Aktorenkonzept
- Fritzson Modelicabücher, Paper
- Modelica By Example
