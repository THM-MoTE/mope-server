package de.thm

import java.nio.file.Path

package object mope {
	type Filter[A] = A => Boolean
	type PathFilter = Filter[Path]
}
