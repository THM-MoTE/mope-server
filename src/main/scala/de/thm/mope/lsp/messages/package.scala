package de.thm.mope.lsp

import java.net.URI

package object messages {
	type DocumentUri = URI
	case class TextDocumentIdentifier (uri: DocumentUri)
	case class TextDocumentPositionParams(textDocument: TextDocumentIdentifier, position: Position)
	case class DidSaveTextDocumentParams(textDocument: TextDocumentIdentifier)
}
