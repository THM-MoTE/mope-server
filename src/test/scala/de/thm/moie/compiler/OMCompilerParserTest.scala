/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm.moie.compiler

import org.scalatest._

class OMCompilerParserTest extends FlatSpec with Matchers {

  "Compiler" should "return no errors if filelist is empty" in {
    val compiler = new OMCompiler(List[String](), "omc", "target")
    compiler.compile(Nil) shouldEqual Nil
  }

  "Compiler errors" should "get parsed" in {

    val compiler = new OMCompiler(List[String](), "omc", "target")

    val msg = """
Error processing file: ../circuit.mo
[/home/mint/Downloads/share/mo/circuit.mo:2:7-2:25:writable] Error: Class Resistor not found in scope circuit.
Error: Error occurred while flattening model circuit

# Error encountered! Exiting...
# Please check the error message and the flags.
""".stripMargin

    compiler.parseErrorMsg(msg) should be (List(
      CompilerError("/home/mint/Downloads/share/mo/circuit.mo",
        FilePosition(2,7), FilePosition(2,25), "Class Resistor not found in scope circuit."
      )))

    val msg3 = """
Error processing file: ../resistor.mo
Failed to parse file: ../resistor.mo!

[/home/mint/Downloads/share/mo/resistor.mo:1:6-12:11:writable] Error: Parse error: The identifier at start and end are different

# Error encountered! Exiting...
# Please check the error message and the flags.
Failed to parse file: ../resistor.mo!
""".stripMargin

    compiler.parseErrorMsg(msg3) should be (List(
      CompilerError(
        "/home/mint/Downloads/share/mo/resistor.mo",
        FilePosition(1,6), FilePosition(12,11), "Parse error: The identifier at start and end are different"
      )))

    val msg4 = """
Error processing file: ../resistor.mo
Failed to parse file: ../resistor.mo!

[/home/mint/Downloads/share/mo/resistor.mo:3:173-3:173:writable] Error: Missing token: ')'

# Error encountered! Exiting...
# Please check the error message and the flags.
Failed to parse file: ../resistor.mo!

Execution failed!
    """.stripMargin

    compiler.parseErrorMsg(msg4) should be (List(
      CompilerError(
        "/home/mint/Downloads/share/mo/resistor.mo",
        FilePosition(3,173), FilePosition(3,173), "Missing token: ')'"
      )))

    val msg5 = """
Error processing file: ../resistor.mo
Notification: Automatically loaded package Modelica 3.2.1 due to uses annotation.
Notification: Automatically loaded package Complex 3.2.1 due to uses annotation.
Notification: Automatically loaded package ModelicaServices 3.2.1 due to uses annotation.
[/home/mint/Downloads/share/mo/resistor.mo:6:3-6:127:writable] Error: Variable resistor1.p not found in scope resistor.
Error: Error occurred while flattening model resistor

# Error encountered! Exiting...
# Please check the error message and the flags.

Execution failed!
    """.stripMargin

    compiler.parseErrorMsg(msg5) should be (List(
      CompilerError(
        "/home/mint/Downloads/share/mo/resistor.mo",
        FilePosition(6,3), FilePosition(6,127), "Variable resistor1.p not found in scope resistor."
      )))

    val msg6 = """
    Error processing file: ../resistor.mo
[/home/mint/Downloads/share/mo/resistor.mo:2:3-2:174:writable] Error: Class Modelica.Electrical.Analog.Basic.Resistor not found in scope resistor.
Error: Error occurred while flattening model resistor

# Error encountered! Exiting...
# Please check the error message and the flags.

Execution failed!

    """.stripMargin

    compiler.parseErrorMsg(msg6) should be (List(
      CompilerError(
        "/home/mint/Downloads/share/mo/resistor.mo",
        FilePosition(2,3), FilePosition(2,174), "Class Modelica.Electrical.Analog.Basic.Resistor not found in scope resistor."
      )))

    val msg7 = """
    Error processing file: ResistorTest.mo
Notification: Automatically loaded package Modelica 3.2.1 due to uses annotation.
Notification: Automatically loaded package Complex 3.2.1 due to uses annotation.
Notification: Automatically loaded package ModelicaServices 3.2.1 due to uses annotation.
[/home/mint/Downloads/share/mo/ResistorTest.mo:7:3-7:115:writable] Error: Incompatible components in connect statement: connect(resistor1.R, resistor2.p)
- resistor1.R has components Real(start = 1.0, quantity = "Resistance", unit = "Ohm")
- resistor2.p has components {i, v}
Error: Error occurred while flattening model ResistorTest

# Error encountered! Exiting...
# Please check the error message and the flags.
    """.stripMargin

    compiler.parseErrorMsg(msg7) should be (List(
      CompilerError(
        "/home/mint/Downloads/share/mo/ResistorTest.mo",
        FilePosition(7,3), FilePosition(7,115), """Incompatible components in connect statement: connect(resistor1.R, resistor2.p) - resistor1.R has components Real(start = 1.0, quantity = "Resistance", unit = "Ohm") - resistor2.p has components {i, v}""".stripMargin
      )))

    val msg8 = """
Error processing file: bouncing_ball.mo
Failed to parse file: bouncing_ball.mo!

[/home/mint/Downloads/bouncing_ball.mo:14:5-14:5:writable] Error: Missing token: THEN

# Error encountered! Exiting...
# Please check the error message and the flags.
Failed to parse file: bouncing_ball.mo!

Execution failed!
    """.stripMargin
    compiler.parseErrorMsg(msg8) should be (List(
      CompilerError(
        "/home/mint/Downloads/bouncing_ball.mo",
        FilePosition(14,5), FilePosition(14,5), "Missing token: THEN")))

val msg9 = """
Error processing file: ResistorTest.mo
Notification: Automatically loaded package Modelica 3.2.1 due to uses annotation.
Notification: Automatically loaded package Complex 3.2.1 due to uses annotation.
Notification: Automatically loaded package ModelicaServices 3.2.1 due to uses annotation.
[/home/mint/Downloads/resistor.mo:6:3-6:104:writable] Error: Incompatible components in connect statement: connect(resistor2.R, ground1.p)
- resistor2.R has components Real(start = 1.0, quantity = "Resistance", unit = "Ohm")
- ground1.p has components {i, v}
Error: Error occurred while flattening model ResistorTest

# Error encountered! Exiting...
# Please check the error message and the flags.

Execution failed!""".stripMargin
    compiler.parseErrorMsg(msg9) should be (List(
      CompilerError(
        "/home/mint/Downloads/resistor.mo",
        FilePosition(6,3), FilePosition(6,104), """Incompatible components in connect statement: connect(resistor2.R, ground1.p) - resistor2.R has components Real(start = 1.0, quantity = "Resistance", unit = "Ohm") - ground1.p has components {i, v}""")))
  }
}
