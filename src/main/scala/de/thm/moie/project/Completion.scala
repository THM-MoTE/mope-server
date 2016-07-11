package de.thm.moie.project

import java.nio.file.Path

import de.thm.moie.compiler.FilePosition

case class Completion(file:String,
                      position:FilePosition,
                      word:String)
