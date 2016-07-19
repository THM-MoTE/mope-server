package de.thm.moie.utils

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.util.function.BinaryOperator

object IOUtils {
  def toString(is:InputStream): String = {
    val bf = new BufferedReader(new InputStreamReader(is))
    bf.lines().reduce("", new BinaryOperator[String]() {
      override def apply(t: String, u: String): String = t+u
    })
  }
}
