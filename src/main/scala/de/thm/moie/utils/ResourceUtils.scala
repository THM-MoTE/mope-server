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

package de.thm.moie.utils

import java.io.Closeable
import java.net.URI
import java.nio.file.{Files, Path, Paths}
import java.util.Base64

import de.thm.moie.Global

object ResourceUtils {

  def getFilename(uri:URI):String = {
    val uriStr = uri.toString
    uriStr.substring(uriStr.lastIndexOf("/")+1, uriStr.length)
  }

  def getFilename(p:Path):String = {
    p.getFileName.toString
  }

  /** Encodes the given bytes as base64 */
  def encodeBase64(bytes:Array[Byte]): Array[Byte] = {
    val encoder = Base64.getEncoder
    encoder.encode(bytes)
  }

  /** Encodes the given bytes as base64 and returns it as a string.
    * @see [ResourceUtils#encodeBase64]
    */
  def encodeBase64String(bytes:Array[Byte]): String =
    new String(encodeBase64(bytes), Global.encoding)

  /** Copies src into the parent-directory of target */
  def copy(src:URI, target:URI): Unit = {
    val targetPath = Paths.get(target).getParent
    val srcPath = Paths.get(src)
    val filename = srcPath.getFileName
    Files.copy(srcPath, targetPath.resolve(filename))
  }

  /** Try-with-resources for Scala. ;-) */
  def tryR[A <: Closeable, B](cl:A)(fn: A => B): B = {
    try {
      fn(cl)
    } finally {
      cl.close()
    }
  }
}
