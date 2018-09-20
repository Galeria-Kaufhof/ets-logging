This library is currently in the prototyping phase. 
Don't expect working code and don't even try to use it!


# `ets-logging-apisandbox` 
This is the place to lay down the target api.
Furthermore, it is used to test some api aspects.
Aspects include: General API Look And Feel, Type inference, Performance, Physical Module Layout, Example Configuration.
To cover all aspects, all required dependencies are available at once in a single sbt build.
Aspects are covered in separate packages as single scala files.
Physical modules that emerge are developed as objects within those files.
Names are still very volatile.


## Basic Idea
Add this to your `build.sbt`:
```
libraryDependencies += "de.kaufhof.ets" %% "ets-logging-core" % "0.1.0-SNAPSHOT"
```

Given some domain objects:
```scala
case class VariantId(value: String)
case class Variant(id: VariantId, name: String)
case class TestClass(a: Int, b: String)
```

Start defining a set of Keys you wish to use within your logs events:
```scala
import de.kaufhof.ets.logging._
import de.kaufhof.ets.logging.syntax._

import java.time.LocalDateTime
import java.util.UUID

object StringKeys extends LogKeysSyntax[String] with DefaultStringEncoders {
  val Logger:        Key[Class[_]] =      Key("logger")      .withImplicitEncoder
  val Level:         Key[Level] =         Key("level")       .withImplicitEncoder
  val Message:       Key[String] =        Key("msg")         .withImplicitEncoder
  val Timestamp:     Key[LocalDateTime] = Key("ts")          .withExplicit(Encoder.fromToString)
  val VariantId:     Key[VariantId] =     Key("variantid")   .withExplicit(Encoder[VariantId].by(_.value))
  val VariantName:   Key[String] =        Key("variantname") .withImplicitEncoder
  val SomeUUID:      Key[UUID] =          Key("uuid")        .withImplicitEncoder
  val RandomEncoder: Key[Random] =        Key("randenc")     .withExplicit(Encoder[Random].by(_.nextInt(100)))
  val RandomEval:    Key[Int] =           Key("randeval")    .withImplicitEncoder
}
```

`LogKeysSyntax[String]` contains a small DSL to setup keys.
That includes all neccessary syntax to specify `Key` s and associate `Encoder` s to it.
`Key` instances require an `id` of type string and an `Encoder`.
`Encoder` s are used to encode values into `String` as indicated by the type parameter in this case.
`DefaultStringEncoders` is a set of `implicit` `Encoder` definitions for the most common types.
Some of the most common types are; `Int`, `Long`, `Double`, `UUID` and so forth.
These most common type `Encoders` facilitate selecting an `Encoder` for the domain specific `Key` s.
Existing `Encoder` instances can be used to derive new `Encoder` instances, like depicted above.

Next provide a configuration combining the previously defined `Keys` and an optional set of `Decomposer` instances.
```scala
object StringLogConfig extends DefaultLogConfig[String, Unit] with DefaultStringEncoders {
  override type Combined = String
  override def combiner: LogEventCombiner[String, String] = StringLogEventCombiner
  override def appender: Appender = StdOutStringLogAppender

  object Decomposers extends Decomposer2DecomposedImplicits[String] {
    import syntax._
    implicit lazy val variantDecomposer: Decomposer[Variant] = variant =>
      Decomposed(
        Keys.VariantId ~> variant.id,
        Keys.VariantName ~> variant.name
    )
  }

  val syntax = ConfigSyntax(StringKeys, Decomposers)
  override def predefKeys: PredefKeys = syntax.Keys
}
```

Provide a logger instance called `log` via mixin into your `class`/`trait`/`object` using the above config.
Then use it to log different attributes in combination with a message:
```
object Main extends StringLogConfig.LogInstance {
  val variant: Variant = ???
  val uuid: UUID = ???

  log.error("some error", variant)
  log.error("some error", variant, Keys.SomeUUID ~> uuid)

  log.error("some error", Keys.jsKey ~> jsValue)
  log.error("some error", Keys.jsKey ~> TestClass(234, "x"))
  log.error("some error", Keys.objKey ~> TestClass(345, "x"))
}
```

Also take look into the short self-contained complete compliable example under:
(test/Main.scala)[ets-logging-usage/src/main/scala/de/kaufhof/ets/logging/test/Main.scala]


## Module Layout
The project is organized as a maven multi module project.
The idea is that the `ets-logging-core` doesn't deliver any dependencies.
Code fragments that require any dependencies are delivered as sub modules.
Any sub module requires the `ets-logging-core` to work properly.
Not all planned sub modules are already available yet.
Artificats available only during prototyping will disappear once the project is stable.

```
<repository-root>
├── ets-logging-apisandbox       !!! only during prototyping !!! organized with sbt
├── ets-logging-parent           parent pom project to share configs between sub modules
├── ets-logging-core             general api without dependencies
├── ets-logging-actor            akka.actor.Actor specific predefined keys and encoders
├── ets-logging-playjson         play.JsValue encoders
├── ets-logging-circejson        circe.Json encoders
├── ets-logging-catsio           cats.effect.IO appender
├── ets-logging-logstash         !!! not available yet !!! slf4j.Marker encoders
├── ets-logging-mdc              !!! not available yet !!! derive decomposer from case classes
├── ets-logging-shapeless        !!! not available yet !!! derive decomposer from case classes
└── ets-logging-usage            example usage combining all modules
```
