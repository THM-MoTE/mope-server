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
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

object FileSystemTree {
	/**
	 * TODO implement using:
	 * if me.isDirectory
	 *  xs = Files#newDirectoryStream { entry =>
	 *  	recurse(entry)
	 *  } toList
	 *  Node(me, xs)
	 * else Leaf(me)
	 * @type {[type]}
	 */
	def tree(root:Path, pathes:List[Path]):TreeLike[Path] = {
		println(root)
		println("relativized: "+pathes.map(root.relativize(_)))
		println("root: "+pathes.map(root.relativize(_).getName(0)))
		null
	}

	def apply(root:Path, rest:List[Path]): TreeLike[Path] = {
		if(Files.isRegularFile(root))
			Leaf(root)
		else {
			//tree(root, rest)
			val visitor = new FileVisitor
			Files.walkFileTree(root, visitor)
			visitor.tree
		}
	}

	class FileVisitor extends SimpleFileVisitor[Path] {
		private var children = List[TreeLike[Path]]()
		var tree:TreeLike[Path] = null
		override def postVisitDirectory(dir: Path, exc:java.io.IOException): FileVisitResult = {
			//println(s"tree: ${if(tree != null) tree.getClass else "null"} childs: $children\n\n\n")
			tree  match {
				case Node(r, childs) =>
				println("a node: \n"+tree)
				tree = Node(dir, children)
				case Leaf(_) => tree = Node(dir, children)
				case null => tree = Node(dir, children)
			}
			FileVisitResult.CONTINUE
		}

		override def preVisitDirectory(dir:Path, attrs:BasicFileAttributes): FileVisitResult = {
			children = Nil
			FileVisitResult.CONTINUE
		}
		override def visitFile(file:Path, attrs:BasicFileAttributes): FileVisitResult = {
			children ::= Leaf(file)
			FileVisitResult.CONTINUE
		}
	}
}
