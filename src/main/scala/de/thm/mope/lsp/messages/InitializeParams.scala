package de.thm.mope.lsp.messages

import java.net.URI

import de.thm.mope.lsp._
import spray.json.JsValue

case class InitializeParams(
	/**
	 * The process Id of the parent process that started
	 * the server. Is null if the process has not been started by another process.
	 * If the parent process is not alive then the server should exit (see exit notification) its process.
	 */
	processId: Int = -1,

	/**
	 * The rootPath of the workspace. Is null
	 * if no folder is open.
	 *
	 * @deprecated in favour of rootUri.
	 */
	rootPath: Option[DocumentUri],

	/**
	 * The rootUri of the workspace. Is null if no
	 * folder is open. If both `rootPath` and `rootUri` are set
	 * `rootUri` wins.
	 */
	rootUri: Option[DocumentUri],

	/**
	 * User provided initialization options.
	 */
	initializationOptions: Option[JsValue],

	/**
	 * The capabilities provided by the client (editor or tool)
	 */
	capabilities: JsValue) {
	def projectFolder:URI = {
		rootUri.orElse(rootPath).get
	}
}
