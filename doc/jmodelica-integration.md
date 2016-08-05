# Integration von JModelica in Mo|E
JModelica ist eine weiterer alternativer Compiler zu OMC.
Er ist in Java geschrieben, Open Source (GPLv3) und bietet ein
Python Interface zur Kommunikation mit dem Compiler Front-/Backend.

- Beide Interaktionen produzieren Exceptions, wenn ein Kompilierfehler auftritt!
- Informationen über den Modelica-Code sind nur über den abstrakten Syntaxbaum
	erhältlich, eine Scripting API mit dem Umfang wie sie OMC bietet gibt es nicht
- Damit die IDE-Features compilerübergreifend funktionieren sollten sie auf einem
  eigenen, unabhängigen Parser basieren. Damit ist man nicht mehr an die Scripting API
  von OMC bzw. dem AST von JModelica gebunden.

## Interaktion über Python
Über das **Python Modul** ist es auf jeden Fall möglich mit dem Compiler zu
interagieren und Kompilierprozesse anzustossen.
Das bedeutet man benötigt Python Skripte die **eine** bestimmte Aufgabe
erledigen und das Ergebnis in strukturierter Weise (am besten JSON) erstellen.
Entweder geschrieben in eine **Datei** oder auf **STDOUT**.

Python scheint primär zur Interaktion während der Simulationsphase zu sein.

## Interaktion über Java
Da der Compiler in Java geschrieben ist, sollte es auch möglich sein
direkt über Java mit ihm zu kommunizieren.
Da wäre zum einen die
[`org.jmodelica.modelica.compiler.ModelicaCompiler`](http://www.jmodelica.org/api-docs/modelica_compiler/classorg_1_1jmodelica_1_1modelica_1_1compiler_1_1_modelica_compiler.html#details)
Klasse als Haupteinstiegspunkt mit der Funktion
 [`org.jmodelica.modelica.compiler.ModelicaCompiler.compileModel()`](http://www.jmodelica.org/api-docs/modelica_compiler/classorg_1_1jmodelica_1_1modelica_1_1compiler_1_1_modelica_compiler.html#a7bcae8980a14a93a0a51d525e1c31267)

`compileModel()` gibt den abstrakten Syntaxbaum zurück, auf dem man weitere
Aktionen ausführen kann.
