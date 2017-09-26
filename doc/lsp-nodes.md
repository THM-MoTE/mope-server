- Useful libraries:
  - https://github.com/MfgLabs/akka-stream-extensions
  - especially `withHead` (can be built using prefixAndTail -> map -> flatMapConcat)
  - https://github.com/dhpcs/scala-json-rpc
  - https://github.com/dragos/dragos-vscode-scala

## Application DSL

``` scala
def as[T:JsonFormat]:JsonFormat
class RpcHandler(method:String, serializer:JsonFormat)
//  methodName,     deserializer (T:JsonFormat)
Lsp.routes( ("document/saved", as[SavedMsg]) ~> Flow[SavedMsg].via(handler)
    | ("document/opened", as[OpenMsg]) ~> Flow[OpenMsg].via(handler2)
)
```
