package de.thm.mope.lsp

package object messages {
	type DocumentUri = String
	case class TextDocumentIdentifier (uri: DocumentUri)
	case class TextDocumentPositionParams(textDocument: TextDocumentIdentifier, position: Position)
	case class DidSaveTextDocumentParams(textDocument: TextDocumentIdentifier)
}
