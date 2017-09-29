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
  import ProtocolHandler._
  val headerRegex = """\s*([\w-]+):\s+([\d\w-\/]+)\s*""".r
  val separator = "\r\n\r\n" //header & payload separator
  val in:Inlet[ByteString] = Inlet("Lsp.in")
  val out:Outlet[String] = Outlet("Lsp.out")
  override val shape:FlowShape[ByteString, String] = FlowShape(in,out)

  override def createLogic(inheritedAttributes: Attributes):GraphStageLogic =
    new GraphStageLogic(shape) with StageLogging {
      var remainingBytes = 0
//      var readPayload = false
      var state:ByteString => Unit = readHeader //are we reading header or payload?
      var headerBuffer = ByteString.empty
      var payloadBuffer = ByteString.empty

      def parseHeaders(headers:ByteString):Headers = {
        headers.utf8String
          .split("\r\n")
          .collect { case headerRegex(k,v) => k -> v }
          .toMap
      }

      /** Reads headers and continues till it receives '\r\n\r\n'.
        * Switches to readPayload afterwards.
        */
      def readHeader(currentBuffer:ByteString): Unit = {
        val currentHeader = headerBuffer ++ currentBuffer
        val idx = currentHeader indexOfSlice separator
        log.debug("reading header: {} idx: {}", currentBuffer.utf8String, idx)

        if(idx<0) { //there's no separator -> still reading header
          headerBuffer ++= currentBuffer
          pull(in)
        } else {
          //split header & payload & continue with reading payload
          val (header, payload) = currentHeader.splitAt(idx)
          headerBuffer ++= header //now contains full header
          log.debug("header: {}",headerBuffer.utf8String)
          val headers = parseHeaders(headerBuffer)
          log.debug("parsed headers: {}",headers)
          remainingBytes = headers("Content-Length").toInt
          state = readPayload
          readPayload(payload)
        }
      }

      /** Reads payload until remainingbytes <0.
        * Switches into readHeaders afterwards.
        */
      def readPayload(currentBuffer:ByteString):Unit = {
        log.debug("reading payload: {}", currentBuffer.utf8String)
        payloadBuffer ++= currentBuffer
        remainingBytes -= currentBuffer.size
        log.debug("payload: {}", payloadBuffer.utf8String)
        if(remainingBytes < 0) {
          //reset buffers & push downstream
          headerBuffer = ByteString.empty
          payloadBuffer = ByteString.empty
          state = readHeader
          push(out, currentBuffer.utf8String)
        } else {
          pull(in)
        }
      }


      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          /*
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
           */
          val buffer = grab(in)
          state(buffer)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          if(!hasBeenPulled(in))
            pull(in)
        }
      })
    }}

object ProtocolHandler {
  type Headers = Map[String,String]
}
