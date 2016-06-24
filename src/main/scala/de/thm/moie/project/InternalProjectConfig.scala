package de.thm.moie.project

import java.util.concurrent.ExecutorService

import akka.util.Timeout

/** Internal representation of a config for actors that work with a project. */
case class InternalProjectConfig(val blockingExecutor: ExecutorService,
                            implicit val defaultTimeout:Timeout) {

}
