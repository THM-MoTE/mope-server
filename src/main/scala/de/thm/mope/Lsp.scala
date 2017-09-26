package de.thm.mope

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import akka.stream.scaladsl._

object Lsp
    extends App {

  val body = """{
	"jsonrpc": "2.0",
	"id": 1,
	"method": "textDocument/didOpen",
	"params": {
		"test":1
	}
}
""".getBytes("UTF-8")
  val header = s"""Content-Type: text/utf-8
Content-Length: ${body.size}

""".getBytes("ASCII")

  val msg = header ++ body //++ header

  // println(new String(msg))

  val stream = new java.io.ByteArrayInputStream(msg)

  implicit val system = ActorSystem("lsp-test")
  implicit val context =  system.dispatcher
  implicit val mat = ActorMaterializer()

  val lengthRegex = """Content-Length:\s+(\d+)""".r

  StreamConverters.fromInputStream(() => stream, 24)
    .via(Framing.delimiter(ByteString("\n"), 8024, true))
    .map(_.utf8String)
  // .map{str => println("inspect: "+str) ; str}
    .splitWhen(_.matches(lengthRegex.regex))
  //fold together until size reached; turn into string & mergeSubstreams
    .drop(1)
    .fold("[sub]: ")((acc,elem) => acc+elem)
    .mergeSubstreams
    .filter(s => s.trim != "[sub]:")
    .runForeach(println)
    .onComplete(_ => system.terminate())

 //  Source(0 until 50)
 //    .splitWhen(_%5==0)
 //    .fold(List.empty[Int])((acc,elem) => elem::acc)
 //    .map(_.reverse)
 //    .mergeSubstreams
 //    .runForeach(println)
 //    .onComplete(_ => system.terminate())
 }
