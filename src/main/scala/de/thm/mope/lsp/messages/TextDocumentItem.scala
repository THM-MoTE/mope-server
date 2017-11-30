package de.thm.mope.lsp.messages

import java.nio.file.{Path, Paths}

case class TextDocumentItem (
	/**
	 * The text document's URI.
	 */
	uri: DocumentUri,

	/**
	 * The text document's language identifier.
	 */
	languageId: String,

	/**
	 * The version number of this document (it will increase after each
	 * change, including undo/redo).
	 */
	version: Int,

	/**
	 * The content of the opened text document.
	 */
	text: String,
) {
  def path:Path = Paths.get(uri)
}
