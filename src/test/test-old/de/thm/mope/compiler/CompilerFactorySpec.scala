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

import scala.collection.JavaConverters._
import java.nio.file.Paths
import com.typesafe.config.ConfigFactory
import org.scalatest._

class CompilerFactorySpec extends FlatSpec with Matchers {
	val tmpDir = Paths.get(System.getProperty("java.io.tmpdir"))
	val outputPath = tmpDir.resolve("target")

	"CompilerFactory" should "return a OpenModelica compiler" in {
		val config = ConfigFactory.parseMap(Map("compiler" -> "omc", "compilerExecutable" -> "omc").asJava)
		val factory = new CompilerFactory(config)
		factory.newCompiler(outputPath) shouldBe a [OMCompiler]
	}

	it should "return a JModelica compiler" in {
		val config = ConfigFactory.parseMap(Map("compiler" -> "jm", "compilerExecutable" -> "IPython").asJava)
		val factory = new CompilerFactory(config)
		factory.newCompiler(outputPath) shouldBe a [JMCompiler]
	}

	it should "throw an exception if compiler-key is unknown" in {
		val config = ConfigFactory.parseMap(Map("compiler" -> "test", "compilerExecutable" -> "test").asJava)
		an [IllegalArgumentException] shouldBe thrownBy (new CompilerFactory(config))
	}
}
