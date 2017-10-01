package de.thm.mope

package object lsp {
	type DocumentUri = String
	case class MethodNotFoundException(s:String) extends Exception(s)
	object ErrorCodes {
		// Defined by JSON RPC
		val ParseError = -32700;
		val InvalidRequest = -32600;
		val MethodNotFound = -32601;
		val InvalidParams = -32602;
		val InternalError = -32603;
		val serverErrorStart = -32099;
		val serverErrorEnd = -32000;
		val ServerNotInitialized = -32002;
		val UnknownErrorCode = -32001;

		// Defined by the protocol.
		val RequestCancelled = -32800;
	}
}
