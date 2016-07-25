package de.thm.moie.doc

import de.thm.moie.doc.DocInfo._

case class DocInfo(info:String,
                   revisions:String,
                   infoHeader:String,
                   subcomponents:Set[Subcomponent])

object DocInfo {
  case class Subcomponent(className:String, classComment:Option[String]) extends Ordered[Subcomponent] {
    override def compare(that: Subcomponent): Int = className compare that.className
  }

  implicit val subcomponentOrdering = new Ordering[Subcomponent] {
    override def compare(x: Subcomponent, y: Subcomponent): Int = x compare y
  }
}
