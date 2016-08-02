package de.thm.moie

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

trait FallbackConfig {
  private val fallbackMap =
    Map("force-english" -> "false",
      "indexFiles" -> "true",
      "exitOnLastDisconnect" -> "false",
      "app.mode" -> "prod",
      "defaultAskTimeout" -> "20",
      "http.interface" -> "127.0.0.1",
      "http.port" -> "9001")

  val fallbackConfig = ConfigFactory.parseMap(fallbackMap.asJava)
}
