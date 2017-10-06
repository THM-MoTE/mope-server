package de.thm.mope.lsp.messages

import de.thm.mope.suggestion.Suggestion

case class CompletionItem(label:String,
                         kind:Int,
                          insertText:String,
                         documentation:Option[String])

object CompletionItem {
  object CompletionItemKind {
    val Text = 1
    val Method = 2
    val Function = 3
    val Constructor = 4
    val Field = 5
    val Variable = 6
    val Class = 7
    val Interface = 8
    val Module = 9
    val Property = 10
    val Unit = 11
    val Value = 12
    val Enum = 13
    val Keyword = 14
    val Snippet = 15
    val Color = 16
    val File = 17
    val Reference = 18
  }

  def apply(word:String, sug:Suggestion):CompletionItem = {
    val kind = sug.kind match {
      case Suggestion.Kind.Variable => CompletionItemKind.Variable
      case Suggestion.Kind.Function => CompletionItemKind.Function
      case Suggestion.Kind.Keyword => CompletionItemKind.Keyword
      case Suggestion.Kind.Package => CompletionItemKind.Module
      case Suggestion.Kind.Property => CompletionItemKind.Property
      case Suggestion.Kind.Model |
           Suggestion.Kind.Class |
           Suggestion.Kind.Type => CompletionItemKind.Class
    }
    val idxDot = sug.name.lastIndexOf(".")
    val insertText = if(idxDot != -1) sug.name.drop(idxDot+1) else sug.name
    CompletionItem(sug.name, kind, insertText, sug.classComment)
  }
}
