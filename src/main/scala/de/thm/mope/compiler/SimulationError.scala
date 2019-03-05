package de.thm.mope.compiler

case class SimulationError(msg:String) extends RuntimeException(msg)
