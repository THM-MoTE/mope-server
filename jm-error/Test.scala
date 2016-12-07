package de.thm.mope

import org.jmodelica.modelica.compiler.ModelicaCompiler.TargetObject
import org.jmodelica.modelica.compiler.SourceRoot


object Test extends App {
  import org.jmodelica.modelica.compiler.ModelicaCompiler
  val compiler = new ModelicaCompiler(ModelicaCompiler.createOptions())
  val target: TargetObject = compiler.createTargetObject("me", "1.0")
  val ast: SourceRoot = compiler.parseModel(Array("test.mo"))
  compiler.instantiateModel(ast, "test", target)
  compiler.compileFMU("test", Array("test.mo"), "me", "test.fmu")
}
