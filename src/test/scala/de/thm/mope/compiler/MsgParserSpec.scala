/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.mope.compiler

import java.nio.file.{Files, Paths}

import de.thm.mope.position.FilePosition
import org.scalatest._

class MsgParserSpec extends FlatSpec with Matchers {
  val projPath = Files.createTempDirectory("moie")

  "Compiler" should "return no errors if filelist is empty" in {
    val compiler = new OMCompiler("omc", projPath.resolve("target"))
    compiler.compile(Nil, Paths.get("")) shouldEqual Nil
    compiler.stop()
  }

  "Compiler errors" should "get parsed" in {

    val compiler = new OMCompiler("omc", projPath.resolve("target"))

    val msg = """
Error processing file: ../circuit.mo
[/home/mint/Downloads/share/mo/circuit.mo:2:7-2:25:writable] Error: Class Resistor not found in scope circuit.
Error: Error occurred while flattening model circuit

# Error encountered! Exiting...
# Please check the error message and the flags.
""".stripMargin

    compiler.parseErrorMsg(msg) should be (List(
      CompilerError("Error", "/home/mint/Downloads/share/mo/circuit.mo",
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
      CompilerError("Error",
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
      CompilerError("Error",
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
      CompilerError("Error",
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
      CompilerError("Error",
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
      CompilerError("Error",
        "/home/mint/Downloads/share/mo/ResistorTest.mo",
        FilePosition(7,3), FilePosition(7,115),
"""Incompatible components in connect statement: connect(resistor1.R, resistor2.p)
- resistor1.R has components Real(start = 1.0, quantity = "Resistance", unit = "Ohm")
- resistor2.p has components {i, v}""".stripMargin
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
      CompilerError("Error",
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
      CompilerError("Error",
        "/home/mint/Downloads/resistor.mo",
        FilePosition(6,3), FilePosition(6,104),
"""Incompatible components in connect statement: connect(resistor2.R, ground1.p)
- resistor2.R has components Real(start = 1.0, quantity = "Resistance", unit = "Ohm")
- ground1.p has components {i, v}""")))

    compiler.stop()
  }

  "Multiple Compiler errors" must "get parsed as list" in {
    val compiler = new OMCompiler("omc", projPath.resolve("target"))
    val msg10 = """
    Error processing file: ../ResistorTest.mo
    Notification: Automatically loaded package Modelica 3.2.1 due to uses annotation.
    Notification: Automatically loaded package Complex 3.2.1 due to uses annotation.
    Notification: Automatically loaded package ModelicaServices 3.2.1 due to uses annotation.
    [/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/Electrical/package.mo:2:1-46:15:readonly] Error: Klasse Anlog.Basic.Ground konnte nicht im Geltungsbereich von Modelica.Electrical gefunden werden.
    [/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/package.mo:2:1-7980:13:readonly] Error: Klasse Electrical.Anlog.Basic.Ground konnte nicht im Geltungsbereich von Modelica gefunden werden.
    [/Users/nico/Documents/mo-tests/ResistorTest.mo:2:3-2:170:writable] Error: Klasse Modelica.Electrical.Anlog.Basic.Ground konnte nicht im Geltungsbereich von ResistorTest gefunden werden.
    Error: Error occurred while flattening model ResistorTest

    # Error encountered! Exiting...
    # Please check the error message and the flags.

    Execution failed!""".stripMargin

      val errors = compiler.parseErrorMsg(msg10)
      errors.size should be (3)
      errors(0) should be (CompilerError("Error",
        "/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/Electrical/package.mo",
        FilePosition(2,1), FilePosition(46,15), "Klasse Anlog.Basic.Ground konnte nicht im Geltungsbereich von Modelica.Electrical gefunden werden."))
      errors(1) should be (CompilerError("Error",
        "/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/package.mo",
        FilePosition(2,1), FilePosition(7980,13), "Klasse Electrical.Anlog.Basic.Ground konnte nicht im Geltungsbereich von Modelica gefunden werden."))
      errors(2) should be (CompilerError("Error",
        "/Users/nico/Documents/mo-tests/ResistorTest.mo",
        FilePosition(2,3), FilePosition(2,170), "Klasse Modelica.Electrical.Anlog.Basic.Ground konnte nicht im Geltungsbereich von ResistorTest gefunden werden."))

      errors should be (List(
        CompilerError("Error",
          "/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/Electrical/package.mo",
          FilePosition(2,1), FilePosition(46,15), "Klasse Anlog.Basic.Ground konnte nicht im Geltungsbereich von Modelica.Electrical gefunden werden."),
        CompilerError("Error",
          "/opt/openmodelica/lib/omlibrary/Modelica 3.2.1/package.mo",
          FilePosition(2,1), FilePosition(7980,13), "Klasse Electrical.Anlog.Basic.Ground konnte nicht im Geltungsbereich von Modelica gefunden werden."),
        CompilerError("Error",
          "/Users/nico/Documents/mo-tests/ResistorTest.mo",
          FilePosition(2,3), FilePosition(2,170), "Klasse Modelica.Electrical.Anlog.Basic.Ground konnte nicht im Geltungsbereich von ResistorTest gefunden werden.")
        )
      )

val msg11 =
"""
Error processing file: test.mo
[/private/var/folders/j2/h5b3j8vs6nx55h3h350pwt400000gn/T/moie5261460089392956577/mo-project/test.mo:3:4-3:13:writable] Error: Klasse Rl konnte nicht im Geltungsbereich von myModel gefunden werden.
Error: Error occurred while flattening model myModel

# Error encountered! Exiting...
# Please check the error message and the flags.

""".stripMargin
    compiler.parseErrorMsg(msg11) should be (List(
        CompilerError("Error",
        "/private/var/folders/j2/h5b3j8vs6nx55h3h350pwt400000gn/T/moie5261460089392956577/mo-project/test.mo",
        FilePosition(3,4), FilePosition(3,13),
        "Klasse Rl konnte nicht im Geltungsbereich von myModel gefunden werden.")
    ))

    compiler.stop()
  }

  "Errors without position" should "get parsed" in {
    val compiler = new OMCompiler("omc", projPath.resolve("target"))
    val msg1 =
"""
Error processing file: /Users/testi/ResistorTest.mo
Notification: Automatically loaded package Modelica 3.2.1 due to uses annotation.
Notification: Automatically loaded package Complex 3.2.1 due to uses annotation.
Notification: Automatically loaded package ModelicaServices 3.2.1 due to uses annotation.
Error: Zu viele Gleichungen - überbestimmtes System. Das Modell hat 24 Gleichung(en) und 23 Variable(n).
Error: pre-optimization module clockPartitioning (simulation) failed.

# Error encountered! Exiting...
# Please check the error message and the flags.

Execution failed!
""".stripMargin
    compiler.parseErrorMsg(msg1) should be (List(
      CompilerError("Error",
        "/Users/testi/ResistorTest.mo",
        FilePosition(0,0), FilePosition(0,0),
        """Zu viele Gleichungen
        |- überbestimmtes System. Das Modell hat 24 Gleichung(en) und 23 Variable(n).""".stripMargin
    )))

    val msg2 =
"""\"\"
|\"\"
|\"Error: Failed to load package ResistorTest (default) using MODELICAPATH /opt/openmodelica/lib/omlibrary:/Users/nico/.openmodelica/libraries/.
|Error: Klasse ResistorTest konnte nicht im Geltungsbereich von <TOP> gefunden werden.
|\"
|""".stripMargin

    compiler.parseErrorMsg(msg2) should be (List(
      CompilerError("Error",
        "",
        FilePosition(0,0), FilePosition(0,0),
        "Klasse ResistorTest konnte nicht im Geltungsbereich von <TOP> gefunden werden."
      )
    ))

    compiler.stop()
  }

  "Notifications inside errors" should "get ignored" in {
    val compiler = new OMCompiler("omc", projPath.resolve("target"))

    val msg = """
      |"[/Users/nico/Documents/mo-tests/build.mos:5:1-5:30:writable] Error: Klasse OpenModelica.Scripting.instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden."
      |"[/Users/nico/Documents/mo-tests/build.mos:5:1-5:30:writable] Error: Klasse instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden."
      |"[/Users/nico/Documents/mo-tests/build.mos:5:1-5:30:writable] Error: Klasse OpenModelica.Scripting.instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden."
      """.stripMargin

    val msg2 = """
    |"Notification: Automatically loaded package Modelica 3.2.1 due to uses annotation."
    |"Notification: Automatically loaded package Complex 3.2.1 due to uses annotation."
    |"Notification: Automatically loaded package ModelicaServices 3.2.1 due to uses annotation."
    """.stripMargin + msg

    val errors2 = compiler.parseErrorMsg(msg2)
    errors2(0) should be (CompilerError("Error",
      "/Users/nico/Documents/mo-tests/build.mos",
      FilePosition(5,1), FilePosition(5,30),
      "Klasse OpenModelica.Scripting.instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden.")
    )

    errors2(1) should be (CompilerError("Error",
      "/Users/nico/Documents/mo-tests/build.mos",
      FilePosition(5,1), FilePosition(5,30),
      "Klasse instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden."))

    errors2(2) should be (CompilerError("Error",
      "/Users/nico/Documents/mo-tests/build.mos",
      FilePosition(5,1), FilePosition(5,30),
      "Klasse OpenModelica.Scripting.instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden."))

      val msg3 = """
      |test;true,false
      |
      |"Notification: Automatically loaded package Modelica 3.2.1 due to uses annotation."
      |"Notification: Automatically loaded package Complex 3.2.1 due to uses annotation."
      |"Notification: Automatically loaded package ModelicaServices 3.2.1 due to uses annotation."
      """.stripMargin
      val errors3 = compiler.parseErrorMsg(msg3)
      errors3.size should be (0)

    compiler.stop()
  }

  "Script errors" should "get parsed" in {
    val compiler = new OMCompiler("omc", projPath.resolve("target"))
    val msg = """
      |false
      |false
      |true
      |true
      |""
      |"bla"
      |"[/Users/nico/Documents/mo-tests/build.mos:5:1-5:30:writable] Error: Klasse OpenModelica.Scripting.instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden."
      |"[/Users/nico/Documents/mo-tests/build.mos:5:1-5:30:writable] Error: Klasse instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden."
      |"[/Users/nico/Documents/mo-tests/build.mos:5:1-5:30:writable] Error: Klasse OpenModelica.Scripting.instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden."
      """.stripMargin

    val errors = compiler.parseErrorMsg(msg)
    errors(0) should be (CompilerError("Error",
      "/Users/nico/Documents/mo-tests/build.mos",
      FilePosition(5,1), FilePosition(5,30),
      "Klasse OpenModelica.Scripting.instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden.")
    )

    errors(1) should be (CompilerError("Error",
      "/Users/nico/Documents/mo-tests/build.mos",
      FilePosition(5,1), FilePosition(5,30),
      "Klasse instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden."))

    errors(2) should be (CompilerError("Error",
      "/Users/nico/Documents/mo-tests/build.mos",
      FilePosition(5,1), FilePosition(5,30),
      "Klasse OpenModelica.Scripting.instntiateModel konnte nicht im Geltungsbereich von <global scope> (looking for a function or record) gefunden werden."))

val msg4 =
"""
true
hans
hey
"[/Users/nico/Documents/moTests2/test.mo:5:0-5:0:writable] Error: Parser error: Unexpected token near: (<EOF>)
Error: Failed to load package moTests2 () using MODELICAPATH /Users/nico/Documents:/opt/openmodelica/lib/omlibrary:/Users/nico/.openmodelica/libraries/.""""

    val errors4 = compiler.parseErrorMsg(msg4)
    errors4.size should be (1)

    errors4.head should be (CompilerError("Error", "/Users/nico/Documents/moTests2/test.mo",
      FilePosition(5,0), FilePosition(5,0), "Parser error: Unexpected token near: (<EOF>)"))

    val msg5 =
    """
    false
    "[/Users/nico/Documents/moTests2/test.mo:2:7-4:8:writable] Error: Parse error: The identifier at start and end are different
    Error: Failed to load package moTests2 () using MODELICAPATH /Users/nico/Documents:/opt/openmodelica/lib/omlibrary:/Users/nico/.openmodelica/libraries/.
    """"

      val errors5 = compiler.parseErrorMsg(msg5)
      errors5.size should be (1)
      errors5.head should be (CompilerError(
        "Error", "/Users/nico/Documents/moTests2/test.mo", FilePosition(2,7), FilePosition(4,8),
        "Parse error: The identifier at start and end are different"))

    compiler.stop()
  }

  "Messages with different path separators" should "get parsed" in {
    System.setProperty("os.name", "Windows")
    val parser = new MsgParser()
    val msg =
      """
      false
      "[C:\nico\Documents\test.mo:2:7-4:8:writable] Error: Parse error: The identifier at start and end are different
      Error: Failed to load package moTests2 () using MODELICAPATH /Users/nico/Documents:/opt/openmodelica/lib/omlibrary:/Users/nico/.openmodelica/libraries/.
      """"

      parser.parse(msg) shouldBe util.Success((Seq(CompilerError(
        "Error", "C:\\nico\\Documents\\test.mo", FilePosition(2,7), FilePosition(4,8),
        "Parse error: The identifier at start and end are different"))))

      System.setProperty("os.name", "Linux")
      val msg2 =
        """
        false
        "[/nico/Documents/test.mo:2:7-4:8:writable] Error: Parse error: The identifier at start and end are different
        Error: Failed to load package moTests2 () using MODELICAPATH /Users/nico/Documents:/opt/openmodelica/lib/omlibrary:/Users/nico/.openmodelica/libraries/.
        """"

        parser.parse(msg2) shouldBe util.Success(Seq(CompilerError(
          "Error", "/nico/Documents/test.mo", FilePosition(2,7), FilePosition(4,8),
          "Parse error: The identifier at start and end are different")))
    }
}
