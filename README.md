This library is currently in the prototyping phase. 
Don't expect working code and don't even try to use it!


#`ets-logging-apisandbox` 
This is the place to lay down the target api.
Furthermore, it is used to test some api aspects.
Aspects include: General API Look And Feel, Type inference, Performance, Physical Module Layout, Example Configuration.
To cover all aspects, all required dependencies are available at once in a single sbt build.
Aspects are covered in separate packages as single scala files.
Physical modules that emerge are developed as objects within those files.
Names are still very volatile.


## Basic Idea
Define your preferred log keys using your domain objects:
```
case class VariantId(value: String)
case class Variant(id: VariantId, name: String)
case class TestClass(a: Int, b: String)

import core._
import logstash._
import playjson._

object Keys {
    val VariantId:   LogKey[VariantId] = LogKey[VariantId] (LogId("variantid"),   Loggable[VariantId].by(_.value))
    val VariantName: LogKey[String]    = LogKey[String]    (LogId("variantname"), Loggable.fromImplicit)
    val SomeUUID:    LogKey[UUID]      = LogKey[UUID]      (LogId("uuid"),        Loggable[UUID].by(_.toString))
    val AkkaSource:  LogKey[Actor]     = LogKey[Actor]     (LogId("akkaSource"),  Loggable.fromImplicit)
    val JsKey:       LogKey[JsValue]   = LogKey[JsValue]   (LogId("json"),        Loggable.fromJsonWriter)
    val ObjKey:      LogKey[TestClass] = LogKey[TestClass] (LogId("json"),        Loggable.fromPlayWrites)
}
```

Provide a logger instance called `log` via mixin into your `class`/`trait`/`object`:
```
object Main extends LogInstance {
  
}
```

Use it to log different attributes in combination with a message:
```
val variant: Variant = ???
val actor: Actor = ???
val uuid: UUID = ???

log.error("some error", variant)
log.error("some error", variant, Keys.SomeUUID ~> uuid)

log.error("some error", Keys.jsKey ~> jsValue)
log.error("some error", Keys.jsKey ~> TestClass(234, "x"))
log.error("some error", Keys.objKey ~> TestClass(345, "x"))

log.error("some error", Keys.akkaSource ~> actor)
log.error("some error", Keys.akkaSource ~> actor.context)
log.error("some error", actor)
```


## Possible Module Layout
```
<repository-root>
├── ets-logging-apisandbox                          (only during prototyping)
├── ets-logging-core                                (general api)
├── ets-logging-logstash        --> core            (slf4j.Marker -> LogstashMarker encodings)
├── ets-logging-playjson        --> core            (play.JsValue,play.Writes -> JsonString encodings)
├── ets-logging-circejson       --> core            (circe.Json,circe.Encoder -> JsonString encodings)
└── ets-logging-shapeless       ???                 (derive keys loggable compounds from case classes)
```