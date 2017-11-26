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
        if(idx<0) { //there's no separator -> still reading header
          headerBuffer ++= currentBuffer
          pull(in)
        } else {
          //split header & payload & continue with reading payload
          val (header, payload) = currentHeader.splitAt(idx)
          headerBuffer ++= header //now contains full header
          val headers = parseHeaders(headerBuffer)
          log.debug("headers: {}",headers)
          remainingBytes = headers("Content-Length").toInt+separator.size //read length given in header + length(\r\n\r\n)
          state = readPayload
          readPayload(payload)
        }
      }

      /** Reads payload until remainingbytes <0.
        * Switches into readHeaders afterwards.
        */
      def readPayload(currentBuffer:ByteString):Unit = {
        //split into payload & possible next msg
        val (head,rest) = currentBuffer.splitAt(remainingBytes)
        payloadBuffer ++= head
        remainingBytes -= currentBuffer.size
        if(remainingBytes <= 0) {
          //payload complete: push downstream, reset buffers & continue reading header from remaining buffer
          emit(out, payloadBuffer.utf8String)
          headerBuffer = ByteString.empty
          payloadBuffer = ByteString.empty
          remainingBytes = 0
          state = readHeader
          readHeader(rest) //read header from remaining buffer
        } else {
          pull(in)
        }
      }


      setHandler(in, new InHandler {
        override def onPush(): Unit = state(grab(in))
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
