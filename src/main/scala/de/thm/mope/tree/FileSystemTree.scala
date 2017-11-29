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


package de.thm.mope.tree

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import de.thm.mope._
import de.thm.mope.utils.ResourceUtils

import scala.collection.JavaConverters._

object FileSystemTree {

  type PTree = TreeLike[Path]

  def add(tree: PTree)(path: Path): PTree = tree match {
    case Leaf(x) => ???
    case Node(me, children) if path.getParent == me =>
      //TODO handle directories
      println(s"found me $me other $path")
      Node(me, Leaf(path) :: children)
    case Node(me, children) =>
      children.find { subTree =>
        path.startsWith(subTree.label)
      }.map {
        case subTree@Node(lbl, _) =>
          val newSubTree = add(subTree)(path)
          println("new subtree\n" + newSubTree.pretty)
          Node(me, newSubTree :: children.filterNot(_ == subTree))
        case l@Leaf(lbl) =>
          println(s"leaf $lbl")
          Node(me, Node(lbl, List(Leaf(path))) :: children.filterNot(_ == l))
      }.get
  }

  private def buildTree(root: Path, filter: DirectoryStream.Filter[Path]): PTree = {
    if (Files.isRegularFile(root))
      Leaf(root)
    else if (Files.isDirectory(root)) {
      ResourceUtils.closeable(Files.newDirectoryStream(root, filter)) { dirStream =>
        val children = (for (entry <- dirStream.asScala) yield {
          if (Files.isDirectory(entry)) buildTree(entry, filter)
          else Leaf(entry)
        }).toList
        Node(root, children)
      }
    } else throw new IllegalArgumentException(s"what is this? $root")
  }

  def apply(root: Path, filter: PathFilter): PTree = {
    buildTree(root, dirFilter(filter))
  }

  def apply(root: Path): PTree = {
    buildTree(root, dirFilter(_ => true))
  }

  def dirFilter(additionalFilter: PathFilter) = new DirectoryStream.Filter[Path]() {
    override def accept(entry: Path): Boolean =
      !Files.isHidden(entry) && additionalFilter(entry)
  }

  class FileVisitor extends SimpleFileVisitor[Path] {
    private var children = List[TreeLike[Path]]()
    var tree: TreeLike[Path] = null

    override def postVisitDirectory(dir: Path, exc: java.io.IOException): FileVisitResult = {
      //println(s"tree: ${if(tree != null) tree.getClass else "null"} childs: $children\n\n\n")
      tree match {
        case Node(r, childs) =>
          println("a node: \n" + tree)
          tree = Node(dir, children)
        case Leaf(_) => tree = Node(dir, children)
        case null => tree = Node(dir, children)
      }
      FileVisitResult.CONTINUE
    }

    override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
      children = Nil
      FileVisitResult.CONTINUE
    }

    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      children ::= Leaf(file)
      FileVisitResult.CONTINUE
    }
  }

}
