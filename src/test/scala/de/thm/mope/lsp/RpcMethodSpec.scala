package de.thm.mope.lsp

import de.thm.mope.MopeSpec
import de.thm.mope.server.JsonSupport

import scala.concurrent.Future

class RpcMethodSpec extends MopeSpec with JsonSupport {
  import RpcMethod._
  "The RpcMethod" should {
    val m1 = request("test"){ i:Int => Future.successful(i*2) }
    val m2 = request("test2"){ s:String => Future.successful(s.toUpperCase) }
    val m3 = request("test3"){ s:String => Future.successful(s.toUpperCase) }

    "combine 2 methods using 'or'" in {
      (m1 | m2) should be (RpcMethod(m1.methodName, Some(m2))(m1.handler))
      (m1 | (m2 | m3)) should be (RpcMethod(m1.methodName, Some(RpcMethod(m2.methodName,Some(m3))(m2.handler)))(m1.handler))
    }
    "return each method name from '.methods'" in {
      m1.methods should be (Set(m1.methodName))
      m2.methods should be (Set(m2.methodName))
      (m1 | (m2 | m3)).methods should be (Set(m1.methodName, m2.methodName, m3.methodName))
    }
  }
}
