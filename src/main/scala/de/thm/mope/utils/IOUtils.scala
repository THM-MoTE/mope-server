/**
  * Copyright (C) 2016,2017 Nicola Justus <nicola.justus@mni.thm.de>
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


package de.thm.mope.utils

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.nio.file.{Files, Path}
import java.util.function.BinaryOperator

object IOUtils {
  def toString(is: InputStream): String = {
    val bf = new BufferedReader(new InputStreamReader(is))
    bf.lines().reduce("", new BinaryOperator[String]() {
      override def apply(t: String, u: String): String = t + "\n" + u
    })
  }

  /**
    * Creates the given directory if it doesn't exist. Unlike Files.createDirectory()
    * it doesn't crash if the directory already exists.
    */
  def createDirectory(directory: Path): Unit = {
    if (!Files.exists(directory))
      Files.createDirectory(directory)
  }
}
