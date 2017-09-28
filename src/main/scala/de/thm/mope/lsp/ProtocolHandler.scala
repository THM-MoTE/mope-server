package de.thm.mope.lsp

import akka.stream._
import akka.stream.stage._
import akka.util.ByteString


/** The stage that handles the underlying Language-Server-Protocol.
  *
  * Parses header & body and provides the body of the message (the json object).
  * Should be used behind line-framing and before a json-serializer.
  */
private[lsp] class ProtocolHandler extends GraphStage[FlowShape[ByteString,String]] {
  val headerRegex = """\s*([\w-]+):\s+([\d\w-\/]+)\s*""".r
  val in:Inlet[ByteString] = Inlet("Lsp.in")
  val out:Outlet[String] = Outlet("Lsp.out")
  override val shape:FlowShape[ByteString, String] = FlowShape(in,out)

  override def createLogic(inheritedAttributes: Attributes):GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {
      var remainingBytes = 0
      var readPayload = false
      var buffer = ByteString.empty


      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val bufferedLine = grab(in)
          val line = bufferedLine.utf8String
          log.debug("In: {}", line)
          line match {
            case headerRegex("Content-Length", n) =>
              //new message with payload size
              remainingBytes = n.toInt
              log.debug("New remaining: {}", remainingBytes)
            case headerRegex(k, v) =>
              //TODO: handle other headers
              //a header we don't care about (currently only encoding = utf-8 is available)
              log.debug("Ignore unknown header: {}", line)
            case s if s.trim.isEmpty =>
              //delimiter between header & payload
              readPayload = true
            case _ if readPayload && remainingBytes > 0 =>
              //actual payload - read until remaining <= 0
              remainingBytes -= (bufferedLine.size + 1) //+ line terminator
              buffer ++= bufferedLine
              log.debug("New Buffer: {} remaining: {}", buffer.utf8String, remainingBytes)
              if (remainingBytes <= 0) {
                  //to utf-8 & kill \r & trim whitespace
                val jsonPayload = buffer.utf8String.replaceAllLiterally("\\r", "")
                //val jsonString = if (jsonPayload.endsWith("}")) jsonPayload else jsonPayload + "}"
                log.debug("final json: {}", jsonPayload)
                buffer = ByteString.empty
                readPayload = false
                push(out, jsonPayload)
              }
          }
          pull(in)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          if(!hasBeenPulled(in))
            pull(in)
        }
      })
    }
}
