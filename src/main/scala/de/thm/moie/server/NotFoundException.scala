package de.thm.moie.server

case class NotFoundException(msg:String) extends Exception(msg)
