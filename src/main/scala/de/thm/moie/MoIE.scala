package de.thm.moie

import de.thm.moie.server.Server


object MoIE {
  def main(args:Array[String]) = {
    println("loaded")
    val port = Global.config.getInt("server-port").getOrElse(9000)
    val server = new Server(port)
  }
}
