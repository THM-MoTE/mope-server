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

package de.thm.mope.tree

import org.scalatest.{Matchers, WordSpec}
import java.nio.file._

class FileSystemTreeSpec extends WordSpec with Matchers {
	"A FSTree" should {
		"create a tree from a filesystem" in {
			val root = Paths.get(System.getProperty("user.home")).resolve("Downloads")

			println( FileSystemTree(root, List()).pretty )
		}
	}
}
