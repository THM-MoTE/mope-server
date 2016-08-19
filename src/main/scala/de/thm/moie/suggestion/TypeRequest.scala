package de.thm.moie.suggestion

import de.thm.moie.position.FilePosition

case class TypeRequest(file:String,
	                     position:FilePosition,
	                     word:String)
