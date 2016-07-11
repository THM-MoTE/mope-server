package de.thm.moie.project

import java.nio.file.Path

import de.thm.moie.compiler.FilePosition

case class Completion(file:Path,
                      position:FilePosition,
                      word:String)
