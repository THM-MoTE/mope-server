package de.thm.mope.lsp

import java.net.URI
import java.nio.file.{Files, Path, Paths}

import de.thm.mope.suggestion.CompletionRequest

package object messages {
  type DocumentUri = URI

  case class TextDocumentIdentifier (uri: DocumentUri, version:Option[Int]) {
    def path:Path = Paths.get(uri)
  }
  case class TextDocumentPositionParams(textDocument: TextDocumentIdentifier, position: Position) {
    def toCompletionRequest:CompletionRequest = {
      val line = Files.readAllLines(Paths.get(textDocument.uri)).get(position.line)
      CompletionRequest(textDocument.uri.getRawPath, position.filePosition, line.take(position.character).trim)
    }
  }
  case class DidSaveTextDocumentParams(textDocument: TextDocumentIdentifier)
  case class DidOpenTextDocumentParams(textDocument: TextDocumentItem)
  case class DidChangeTextDocumentParams(textDocument: TextDocumentIdentifier, contentChanges:Seq[TextDocumentContentChangeEvent])
  case class TextDocumentContentChangeEvent(range:Option[Range], rangeLength:Option[Int], text:String)
  case class MarkedString(language:String,value:String)
  case class Hover(contents:Seq[String], range:Option[Range]=None)
}
