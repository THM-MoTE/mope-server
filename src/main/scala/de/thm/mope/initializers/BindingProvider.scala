package de.thm.mope.initializers

import de.thm.mope.utils.BindingWrapper

import scala.concurrent.Future

trait BindingProvider {
  def provide():Future[BindingWrapper]
}
