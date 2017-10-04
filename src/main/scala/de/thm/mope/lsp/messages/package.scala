package de.thm.mope.lsp

import java.net.URI
import java.nio.file.{Files, Paths}

import de.thm.mope.suggestion.CompletionRequest

package object messages {
	type DocumentUri = URI
	case class TextDocumentIdentifier (uri: DocumentUri)
	case class TextDocumentPositionParams(textDocument: TextDocumentIdentifier, position: Position) {
		def toCompletionRequest:CompletionRequest = {
			val line = Files.readAllLines(Paths.get(textDocument.uri)).get(position.line)
			CompletionRequest(textDocument.uri.getRawPath, position.filePosition, line.take(position.character).trim)
		}
	}
	case class DidSaveTextDocumentParams(textDocument: TextDocumentIdentifier)
}
