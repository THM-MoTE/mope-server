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

object ModelicaProjectTree {

  val packageFilename = "package.mo"

  def packageMoDirectories(tree: TreeLike[Path]): List[Path] =
    tree match {
      case Leaf(x) if x.endsWith(packageFilename) => List(x.getParent)
      case Leaf(x) => Nil
      case Node(me, children) if children.exists { x => x.label.endsWith(packageFilename) } => List(me)
      case Node(me, children) => children.flatMap(packageMoDirectories)
    }

  def singleFiles(tree: TreeLike[Path]): List[Path] =
    singleFiles(tree, packageMoDirectories(tree))

  def singleFiles(tree: TreeLike[Path], packageDirectories: List[Path]): List[Path] = {
    tree.filterElements { path =>
      !packageDirectories.forall(path.startsWith)
    }.filterNot(Files.isDirectory(_))
  }
}
