/**
 * Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 */

package de.thm

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

package object moie {
  def removeDirectoryTree(root:Path): Unit = {
    Files.walkFileTree(root, new SimpleFileVisitor[Path]() {

      override def visitFile(file:Path, attrs:BasicFileAttributes):FileVisitResult = {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      override def postVisitDirectory(dir:Path, exc:java.io.IOException):FileVisitResult = {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
