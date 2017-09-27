package de.thm.mope

import akka.actor.ActorSystem
import akka.stream._
import akka.util.ByteString
import akka.stream.scaladsl._
import spray.json._
import com.typesafe.config.ConfigFactory
import de.thm.mope.lsp.{LspServer, RpcMsg}

object Lsp
    extends App
    with server.JsonSupport
    with lsp.Routes {

  implicit val system = ActorSystem("lsp-test", ConfigFactory.load("/mope.conf"))
  implicit val context =  system.dispatcher
  implicit val mat = ActorMaterializer()

  val server = new LspServer()

  val pipeline = server.connectTo(routes)


  Source(List(RpcMsg(1, "compile", 50.toJson), RpcMsg(2, "complete", "nico".toJson)))
    .map { msg =>
      ByteString(s"""
         |Content-Type: text/utf-8
         |Content-Length: 58
         |
         |${msg.toJson}
       """.stripMargin)
    }
    .via(pipeline)
    .map(_.utf8String)
    .runForeach(println)
    .onComplete(_ => system.terminate())

/*
  case class RpcMsg(jsonrpc:String, id:Int, method:String, params:JsValue)
  implicit val rpcFormat = jsonFormat4(RpcMsg)

  val rpc = RpcMsg("2", 1, "open", JsNumber(3))

  val body = rpc.toJson.toString
  val header = s"""Content-Type: text/utf-8
Content-Length: ${body.size}

$body
"""

  val msg = header.getBytes("UTF-8")

  val stream = new java.io.ByteArrayInputStream(msg)

  val decider: Supervision.Decider = { e =>
    system.log.error("Unhandled exception in stream", e)
    Supervision.Resume
  }

  implicit val system = ActorSystem("lsp-test", ConfigFactory.load("/mope.conf"))
  implicit val context =  system.dispatcher
  implicit val mat = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(decider))

  val lengthRegex = """Content-Length:\s+(\d+)""".r
  val headerRegex = """([\w-]+):\s+([\d\w-/]+)""".r

  val rpcParser = Flow[String].fold("")((acc,elem) => acc+elem)
    //.filterNot(_.trim.isEmpty)
    .map{s => println(s"before-json $s"); s}
    .map { s => s.parseJson.convertTo[RpcMsg] }
    .map{o => println(s"got obj $o"); o}
    .map{o => o.params.convertTo[Int] }
    .map{i => println(s"number $i"); i }

  val consumeMsg = Flow.fromGraph(GraphDSL.create() { implicit builder:GraphDSL.Builder[akka.NotUsed] =>
    import GraphDSL.Implicits._
    val bcast = builder.add(Broadcast[String](2))
    val header = builder.add(Flow[String].takeWhile(_.matches(headerRegex.regex)).to(Sink.ignore))
    val body = builder.add(
      Flow[String].fold("")((acc,elem) => acc+elem)
      .filter(_.trim.isEmpty)
      .map(s => rpcFormat.read(s.toJson))
    )
    bcast ~> header
    bcast ~> body
    FlowShape(bcast.in, body.out)
  })

  StreamConverters.fromInputStream(() => stream)
    .via(Framing.delimiter(ByteString("\n"), 8024, true))
    .map(_.utf8String)
   .map{str => println("inspect: "+str) ; str}
    .splitWhen(_.matches(lengthRegex.regex))
      .dropWhile(s => s.matches(headerRegex.regex) || s.trim.isEmpty)
      .map{str => println("after-split: "+str) ; str}
      //.log("after-drop")
    //fold together until size reached; turn into string & mergeSubstreams
      .via(rpcParser)
    .mergeSubstreams
    .runForeach(println)
    .onComplete(_ => system.terminate())
 */
 //  Source(0 until 50)
 //    .splitWhen(_%5==0)
 //    .fold(List.empty[Int])((acc,elem) => elem::acc)
 //    .map(_.reverse)
 //    .mergeSubstreams
 //    .runForeach(println)
 //    .onComplete(_ => system.terminate())
 }
