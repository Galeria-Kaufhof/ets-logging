package de.kaufhof.ets.logging.stdout

import java.time.LocalDateTime
import java.util.UUID

import cats.effect.IO
import de.kaufhof.ets.logging.stdout.Api._

import scala.language.implicitConversions
import scala.util.Random

object Api {
  object util {
    // TODO: move this into a private internal utility package, maybe as an implicit class on Seq or List
    def findValueByKey[A, B](m: Map[A, B])(p: A => Boolean): Option[B] = m.collectFirst {
      case (pattern, value) if p(pattern) => value
    }
    type Lazy[A] = () => A
    object Lazy {
      def apply[A](value: => A): Lazy[A] = value _
    }
  }

  import util._

  sealed abstract class Level(val priority: Int) extends Ordering[Level] {
    override def compare(x: Level, y: Level): Int = x.priority compare y.priority
    def forwards(other: Level): Boolean = this <= other
  }
  object Level {
    case object Trace extends Level(0)
    case object Debug extends Level(1)
    case object Info extends Level(2)
    case object Warn extends Level(3)
    case object Error extends Level(4)

    val Lowest: Trace.type = Trace
    val Highest: Error.type = Error
  }
  // TODO: move all types prefixed with a Log prefix or with any combination of type parameters E, C, O into a "generic" sub package
  // TODO: evaluate if self types or inheritance is more appropriate except for LogAttribute which is modelled as sealed trait on purpose
  trait LogAppender[C, O] extends LogCombinedTypeDefinition[C] {
    self =>
    def append(combined: Combined): O
    def ignore: O
    def comap[I](f: I => Combined): LogAppender[I, O] = new LogAppender[I, O] {
      override def append(combined: I): O = self.append(f(combined))
      override def ignore: O = self.ignore
    }
  }
  object StdOutStringLogAppender extends LogAppender[String, Unit] {
    override def append(combined: String): Unit = println(combined)
    override def ignore: Unit = ()
  }
  import cats.effect.IO
  object CatsIoAppender extends LogAppender[String, IO[Unit]] {
    override def append(combined: String): IO[Unit] = IO(println(combined))
    override def ignore: IO[Unit] = IO(())
  }

  trait LogEncoder[I, O] { def encode(value: I): O }

  sealed trait LogAttribute[E]
  sealed trait LogPrimitive[I, E] extends LogAttribute[E] {
    def key: LogKey[I, E]
    def evaluated: I
    def encoded: E
    protected def encode: E = key.encoder.encode(evaluated)
  }
  case class LazyPrimitive[I, E](key: LogKey[I, E], value: Lazy[I]) extends LogPrimitive[I, E] {
    override lazy val evaluated: I = value.apply()
    override lazy val encoded: E = encode
  }
  case class EagerPrimitive[I, E](key: LogKey[I, E], value: I) extends LogPrimitive[I, E] {
    override val evaluated: I = value
    override val encoded: E = encode
  }
  case class LogDecomposed[I, E](primitives: LogPrimitive[_, E]*) extends LogAttribute[E]

  case class LogKey[I, E](id: String, encoder: LogEncoder[I, E]) {
    def ->(value: I): EagerPrimitive[I, E] = EagerPrimitive(this, value)
    def ~>(value: => I): LazyPrimitive[I, E] = LazyPrimitive(this, Lazy(value))
  }
  // TODO: try to split these up because not every user might want to log all of these or just wants to use different types
  // TODO: maybe it suffices to introduce 2 more type parameters (or abstract types) to give the caller the freedom to
  // TODO: choose a DateType and a LoggerType which now default to LocalDateTime and Class[_]
  trait LogPredefKeys[E] {
    val Logger: LogKey[Class[_], E]
    val Level: LogKey[Level, E]
    val Message: LogKey[String, E]
    val Timestamp: LogKey[LocalDateTime, E]
  }
  trait Logger[E, O] {
    def event(attrs: LogAttribute[E]*): O
    def trace(msg: String, attrs: LogAttribute[E]*): O
    def debug(msg: String, attrs: LogAttribute[E]*): O
    def info(msg: String, attrs: LogAttribute[E]*): O
    def warn(msg: String, attrs: LogAttribute[E]*): O
    def error(msg: String, attrs: LogAttribute[E]*): O
  }

  type LogEvent[E] = Map[LogKey[_, E], LogPrimitive[_, E]]

  trait LogEventCombiner[E, C] extends LogTypeDefinitions[E] with LogCombinedTypeDefinition[C] {
    def combine(e: Event): Combined
  }
  trait PairLogEventCombiner[E, C] extends LogEventCombiner[E, C] with LogCombinedTypeDefinition[C] {
    type Pair

    protected def primToPair[I](p: Primitive[I]): Pair
    protected def combinePairs(encoded: Seq[Pair]): Combined
    private def toPrims(m: Event): Prims = m.map(_._2)(scala.collection.breakOut)
    override def combine(e: LogEvent[Encoded]): Combined = {
      val prims: Prims = toPrims(e)
      val encoded: Seq[Pair] = prims.map(primToPair(_))
      combinePairs(encoded)
    }
  }
  object StringLogEventCombiner extends PairLogEventCombiner[String, String] {
    override type Pair = String
    override protected def primToPair[I](p: Primitive[I]): Pair = s"${p.key.id} -> ${p.encoded}"
    override protected def combinePairs(encoded: Seq[Pair]): Combined = encoded.sorted.mkString(" | ")
  }

  trait ClassNameExtractor {
    def getClassName(cls: Class[_]): String = cls.getSimpleName
  }
  trait PredefKeysInstance[E] {
    def predefKeys: LogPredefKeys[E]
  }

  trait LogEventFilter[E] {
    def forwards(event: LogEvent[E]): Boolean
  }
  trait ClassLevelLogEventFilter[E]
      extends LogEventFilter[E]
      with ClassNameExtractor
      with PredefKeysInstance[E]
      with LogTypeDefinitions[E] {
    def rootLevel: Level = Level.Info
    def classNameLevels: Map[String, Level] = Map.empty

    def configuredClassLevel(cls: Class[_]): Option[Level] = {
      val name = getClassName(cls)
      findValueByKey(classNameLevels)(name.startsWith)
    }

    // event  root   class  || keep
    // -      error  -      || true   => !event.isDefined
    // -      info   -      || true
    // -      debug  -      || true
    // info   error  -      || false  => root <= event
    // info   info   -      || true
    // info   debug  -      || true
    // info   error  error  || false  => class <= event
    // info   info   error  || false
    // info   debug  error  || false
    // info   error  info   || true   => class <= event
    // info   info   info   || true
    // info   debug  info   || true
    def forwards(event: LogEvent[E]): Boolean = {
      def getPrimitive[I](key: LogKey[I, E]): Option[Primitive[I]] =
        event.get(key).map(_.asInstanceOf[LogPrimitive[I, E]])
      // TODO: avoid asInstanceOf if possible without shapeless
      def evaluatedPrimitive[I](key: Key[I]): Option[I] = getPrimitive(key).map(_.evaluated)
      val eventLevel: Level = evaluatedPrimitive(predefKeys.Level).getOrElse(Level.Highest)
      val configLevel: Level = evaluatedPrimitive(predefKeys.Logger).flatMap(configuredClassLevel).getOrElse(rootLevel)
      configLevel forwards eventLevel
    }
  }
  trait LogAttributeGatherer[E] {
    def gatherGlobal: Seq[LogAttribute[E]]
  }
  trait DefaultAttributeGatherer[E]
      extends LogTypeDefinitions[E]
      with LogAttributeGatherer[E]
      with PredefKeysInstance[E] {
    override def gatherGlobal: Seq[Attribute] = Seq(
      predefKeys.Timestamp ~> LocalDateTime.now()
    )
  }

  trait LogEventOps[E] extends LogTypeDefinitions[E] {
    private def primsToEvent(prims: Prims): Event = prims.map(p => (p.key, p)).toMap
    def fromAttributes(attributes: Seq[Attribute]): Event = primsToEvent(attributesToPrims(attributes))
    def aggregate(acc: Event, next: Event): Event = acc ++ next
    private def attributesToPrims(attributes: Seq[Attribute]): Prims = {
      val zero: Prims = Seq.empty
      attributes.foldLeft(zero) { (acc, attr) =>
        attr match {
          case p: Primitive[_] => acc :+ p
          case c: Composite[_] => acc ++ c.primitives
        }
      }
    }
  }

  trait LogAttributeProcessor[E, O] {
    def process(attributes: Seq[LogAttribute[E]]): O
  }
  trait AbstractLogAttributeProcessor[E, O]
      extends LogAttributeProcessor[E, O]
      with LogTypeDefinitionsExt[E, O]
      with LogEventFilter[E]
      with LogAttributeGatherer[E] {
    type Combined
    final type EventCombiner = LogEventCombiner[Encoded, Combined]
    final type Appender = LogAppender[Combined, Output]

    def combiner: EventCombiner
    def appender: Appender

    private object Event extends LogEventOps[Encoded]

    // TODO: describe this process in the readme:
    // TODO:   - types: Attribute, Primitive, Event, Decomposer
    // TODO:   - attribute scopes: local, global (still missing: logger, logger implicits, local implicits)
    // TODO:     maybe rename local to statement because statement is the "real" scope
    // TODO:   - processing pipeline
    // TODO:     log events can be generated from any amount of attributes
    // TODO:     attributes -> primitives -> primitives with keys => map = event
    // TODO:   - merge semantics
    // TODO:     local attributes overwrite more global attributes
    // TODO:     (still missing: explicit attributes overwrite implicit attributes)
    // TODO:
    def process(attributes: Seq[Attribute]): O = {
      val local = Event.fromAttributes(attributes)
      if (forwards(local)) {
        val global = Event.fromAttributes(gatherGlobal)
        val merged = Event.aggregate(global, local)
        appender.append(combiner.combine(merged))
      } else appender.ignore
    }
  }

  trait LogEncoderOps[E] extends LogTypeDefinitions[E] {
    class EncoderValueHalfApplied[A] {
      def by[B](f: A => B)(implicit derivedFrom: Encoder[B]): Encoder[A] =
        value => derivedFrom.encode(f(value))
    }
    def apply[A]: EncoderValueHalfApplied[A] = new EncoderValueHalfApplied[A]
    def fromImplicit[A: Encoder]: Encoder[A] = Predef.implicitly
    def fromToString[A](implicit e: Encoder[String]): Encoder[A] = apply[A].by[String](_.toString)
  }

  trait LogKeySyntax[E] extends LogTypeDefinitions[E] {
    object Key {
      def apply(id: String): KeyOps = new KeyOps(id)
    }
    class KeyOps(id: String) {
      def withExplicit[I](encoder: Encoder[I]): Key[I] = LogKey(id, encoder)
      def withImplicitEncoder[I](implicit encoder: Encoder[I]): Key[I] = LogKey(id, encoder)
    }
    object Decomposed {
      def apply[I](primitives: Primitive[_]*): Decomposed[I] = LogDecomposed(primitives: _*)
    }
  }

  trait LogEncoderSyntax[E] extends LogTypeDefinitions[E] {
    def encodeString(string: String): Encoded
    final implicit lazy val stringEncoder: Encoder[String] = encodeString
    object Encoder extends LogEncoderOps[Encoded]
  }

  trait LogTypeDefinitions[E] {
    final type Encoded = E
    final type Encoder[I] = LogEncoder[I, Encoded]
    final type Primitive[I] = LogPrimitive[I, Encoded]
    final type Key[I] = LogKey[I, Encoded]
    final type Decomposed[I] = LogDecomposed[I, Encoded]
    final type PredefKeys = LogPredefKeys[Encoded]
    final type Attribute = LogAttribute[Encoded]
    final type Decomposer[I] = LogEncoder[I, Decomposed[I]]
    final type EventFilter = LogEventFilter[Encoded]
    final type AttributeGatherer = LogAttributeGatherer[Encoded]
    final type Composite[I] = LogDecomposed[I, Encoded]
    final type Event = LogEvent[Encoded]

    protected final type Prims = Seq[LogPrimitive[_, Encoded]]
  }
  trait LogTypeDefinitionsExt[E, O] extends LogTypeDefinitions[E] {
    type Output = O
    type Logger = Api.Logger[Encoded, Output]
  }
  trait LogCombinedTypeDefinition[C] {
    final type Combined = C
  }

  trait Decomposer2DecomposedImplicits[E] extends LogTypeDefinitions[E] {
    implicit def anyDecomposer2decomposed[I: Decomposer](obj: I): Decomposed[I] = implicitly[Decomposer[I]].encode(obj)
  }

  trait LoggerFactory[E, O] extends LogTypeDefinitionsExt[E, O] {
    def createLogger(cls: Class[_]): Logger
    trait LogInstance {
      protected lazy val log: Logger = createLogger(getClass)
    }
  }

  trait DefaultLoggerFactory[E, O]
      extends LoggerFactory[E, O]
      with PredefKeysInstance[E]
      with LogAttributeProcessor[E, O] {
    def createLogger(cls: Class[_]): Logger = new Logger {
      private lazy val clsAttr = predefKeys.Logger -> cls
      private def generic(attributes: Seq[LogAttribute[E]]): Output = process(attributes :+ clsAttr)
      final override def event(attrs: LogAttribute[E]*): Output = generic(attrs)
      final override def trace(msg: String, attrs: LogAttribute[Encoded]*): Output =
        generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Trace)
      final override def debug(msg: String, attrs: LogAttribute[E]*): Output =
        generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Debug)
      final override def info(msg: String, attrs: LogAttribute[E]*): Output =
        generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Info)
      final override def warn(msg: String, attrs: LogAttribute[Encoded]*): Output =
        generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Warn)
      final override def error(msg: String, attrs: LogAttribute[E]*): Output =
        generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Error)
    }
  }

  trait DefaultEncoders[E] extends LogTypeDefinitions[E] with LogEncoderSyntax[E] with ClassNameExtractor {
    def createIntEncoder: Encoder[Int]
    def createLongEncoder: Encoder[Long]
    def createFloatEncoder: Encoder[Float]
    def createDoubleEncoder: Encoder[Double]
    def createCharEncoder: Encoder[Char]
    def createByteEncoder: Encoder[Byte]
    def createUuidEncoder: Encoder[UUID]
    def createLevelEncoder: Encoder[Level]
    def createClassEncoder: Encoder[Class[_]] = Encoder[Class[_]].by(getClassName)

    implicit lazy val intEncoder: Encoder[Int] = createIntEncoder
    implicit lazy val longEncoder: Encoder[Long] = createLongEncoder
    implicit lazy val floatEncoder: Encoder[Float] = createFloatEncoder
    implicit lazy val doubleEncoder: Encoder[Double] = createDoubleEncoder
    implicit lazy val charEncoder: Encoder[Char] = createCharEncoder
    implicit lazy val byteEncoder: Encoder[Byte] = createByteEncoder
    implicit lazy val uuidEncoder: Encoder[UUID] = createUuidEncoder
    implicit lazy val levelEncoder: Encoder[Level] = createLevelEncoder
    implicit lazy val classEncoder: Encoder[Class[_]] = createClassEncoder
  }

  trait DefaultStringEncoders extends DefaultEncoders[String] {
    override def encodeString(string: String): Encoded = string
    override def createIntEncoder: Encoder[Int] = Encoder.fromToString
    override def createLongEncoder: Encoder[Long] = Encoder.fromToString
    override def createFloatEncoder: Encoder[Float] = Encoder.fromToString
    override def createDoubleEncoder: Encoder[Double] = Encoder.fromToString
    override def createCharEncoder: Encoder[Char] = Encoder.fromToString
    override def createByteEncoder: Encoder[Byte] = Encoder.fromToString
    override def createUuidEncoder: Encoder[UUID] = Encoder.fromToString
    override def createLevelEncoder: Encoder[Level] = Encoder.fromToString
  }

  trait LogKeysSyntax[E]
      extends LogPredefKeys[E]
      with LogTypeDefinitions[E]
      with LogKeySyntax[E]
      with LogEncoderSyntax[E]
      with ClassNameExtractor

  // TODO: specify minimal base trait for any kind of LogConfig
  trait DefaultLogConfig[E, O]
      extends LogKeySyntax[E]
      with LoggerFactory[E, O]
      with DefaultLoggerFactory[E, O]
      with AbstractLogAttributeProcessor[E, O]
      with ClassLevelLogEventFilter[E]
      with LogAttribute[E]
      with DefaultEncoders[E]
      with DefaultAttributeGatherer[E]
      with ClassNameExtractor

  class ConfigSyntax[K, D](createKeys: Lazy[K], createDecomposers: Lazy[D]) {
    val Keys: K = createKeys()
    val decomposers: D = createDecomposers()
  }
  object ConfigSyntax {
    def apply[K, D](keys: => K, decomposers: => D) = new ConfigSyntax[K, D](keys _, decomposers _)
  }

  object actor {
    import akka.actor._
    trait ActorEncoders[E] extends DefaultEncoders[E] {
      def createActorPathEncoder: Encoder[ActorPath] = Encoder.fromToString
      implicit lazy val actorPathEncoder: Encoder[ActorPath] = createActorPathEncoder
    }
    trait ActorDecomposers[E] extends Decomposer2DecomposedImplicits[E] with LogKeySyntax[E] {
      implicit val actorDecomposer: Decomposer[Actor]
      implicit def anyActor2Decomposer[A <: Actor]: Decomposer[A] =
        actor => Decomposed(actorDecomposer.encode(actor).primitives: _*)
    }
    trait ActorPredefKeys[E] {
      val ActorSource: LogKey[ActorPath, E]
    }
    trait ActorLogKeysSyntax[E] extends ActorPredefKeys[E] with ActorEncoders[E]
  }

  object playjson {
    import play.api.libs.json._
    object JsValueLogEventCombiner extends PairLogEventCombiner[JsValue, JsValue] {
      override type Pair = (String, JsValue)
      override protected def primToPair[I](p: Primitive[I]): Pair = p.key.id -> p.encoded
      override protected def combinePairs(encoded: Seq[Pair]): Combined = JsObject(encoded)
    }
    trait DefaultJsValueEncoders extends DefaultEncoders[JsValue] {
      implicit class EncoderOps(e: Encoder.type) {
        def fromPlayJsonWrites[I](implicit w: Writes[I]): Encoder[I] = w.writes
      }
      override def encodeString(string: String): Encoded = JsString(string)

      override def createIntEncoder: Encoder[Int] = Encoder.fromPlayJsonWrites
      override def createLongEncoder: Encoder[Long] = Encoder.fromPlayJsonWrites
      override def createFloatEncoder: Encoder[Float] = Encoder.fromPlayJsonWrites
      override def createDoubleEncoder: Encoder[Double] = Encoder.fromPlayJsonWrites
      override def createCharEncoder: Encoder[Char] = Encoder[Char].by(_.toByte)
      override def createByteEncoder: Encoder[Byte] = Encoder.fromPlayJsonWrites
      override def createUuidEncoder: Encoder[UUID] = Encoder.fromPlayJsonWrites
      override def createLevelEncoder: Encoder[Level] = Encoder.fromToString
    }
  }
  object circejson {
    import io.circe
    import io.circe.Json
    object JsonLogEventCombiner extends PairLogEventCombiner[Json, Json] {
      override type Pair = (String, Json)
      override protected def primToPair[I](p: Primitive[I]): Pair = p.key.id -> p.encoded
      override protected def combinePairs(encoded: Seq[Pair]): Combined = Json.fromFields(encoded)
    }
    trait DefaultJsonEncoders extends DefaultEncoders[Json] {
      implicit class EncoderOps(e: Encoder.type) {
        def fromCirceJsonEncoder[I](implicit w: circe.Encoder[I]): Encoder[I] = w.apply
      }
      override def encodeString(string: String): Encoded = Json.fromString(string)

      override def createIntEncoder: Encoder[Int] = Encoder.fromCirceJsonEncoder
      override def createLongEncoder: Encoder[Long] = Encoder.fromCirceJsonEncoder
      override def createFloatEncoder: Encoder[Float] = Encoder.fromCirceJsonEncoder
      override def createDoubleEncoder: Encoder[Double] = Encoder.fromCirceJsonEncoder
      override def createCharEncoder: Encoder[Char] = Encoder[Char].by(_.toByte)
      override def createByteEncoder: Encoder[Byte] = Encoder.fromCirceJsonEncoder
      override def createUuidEncoder: Encoder[UUID] = Encoder.fromCirceJsonEncoder
      override def createLevelEncoder: Encoder[Level] = Encoder.fromToString
    }

  }

  object logstash {
    import net.logstash.logback.marker.Markers
    import org.slf4j.Marker
    // TODO: provide SLF4J based event filter taking the underlying configuration into account
    object Slf4jLogstashMarkerLogEventCombiner extends LogEventCombiner[Marker, Marker] {
      override def combine(e: LogEvent[Encoded]): Marker = {
        import scala.collection.JavaConverters._
        val zero: Marker = Markers.appendEntries(Map.empty.asJava)
        e.foldLeft(zero) { (acc, marker) =>
          // TODO: this is a workaround. why does acc.and(marker) throw a class cast exception?
          acc.add(marker._2.encoded)
          acc
        }
      }
    }
  }
}

object Domain {
  case class VariantId(value: String)
  case class Variant(id: VariantId, name: String)
  val variant: Variant = Variant(VariantId("VariantId"), "VariantName")
  val uuid: UUID = java.util.UUID.fromString("723f03f5-13a6-4e46-bdac-3c66718629df")

  object encoding {
    object string {
      object StringKeys extends LogKeysSyntax[String] with DefaultStringEncoders {
        val Logger: Key[Class[_]] = Key("logger").withImplicitEncoder
        val Level: Key[Level] = Key("level").withImplicitEncoder
        val Message: Key[String] = Key("msg").withImplicitEncoder
        val Timestamp: Key[LocalDateTime] = Key("ts").withExplicit(Encoder.fromToString)
        val VariantId: Key[VariantId] = Key("variantid").withExplicit(Encoder[VariantId].by(_.value))
        val VariantName: Key[String] = Key("variantname").withImplicitEncoder
        val SomeUUID: Key[UUID] = Key("uuid").withImplicitEncoder
        val RandomEncoder: Key[Random] = Key("randenc").withExplicit(Encoder[Random].by(_.nextInt(100)))
        val RandomEval: Key[Int] = Key("randeval").withImplicitEncoder
      }

      object StringLogConfig extends Api.DefaultLogConfig[String, Unit] with DefaultStringEncoders {
        override type Combined = String
        override def combiner: LogEventCombiner[String, String] = StringLogEventCombiner
        override def appender: Appender = StdOutStringLogAppender
        override val classNameLevels: Map[String, Level] = Map(
          "Asd" -> Level.Debug
        )

        // TODO: avoid duplication of syntax and Keys definition, lookup other config to see duplication
        // TODO: it is not so easy my first attempt resulted in intellij being confused about the Keys object
        // TODO: or it could not find the general decomposer implicits being mixed in
        val syntax = ConfigSyntax(StringKeys, Decomposers)
        override def predefKeys: PredefKeys = syntax.Keys

        // TODO: here is a chance to reduce duplication, any decomposer depends on the domain, its keys and the encoding
        // TODO: the encoding isn't exactly specific to the decomposers, it should be possible to define decomposers
        // TODO: according to only knowing the keys relative to an encoded type E
        // TODO: maybe we just need another type K for the Keys the user is going to provide like this:
        // TODO:     trait LogDecomposers[K] {
        // TODO:       type Keys = K
        // TODO:       val Keys: Keys
        // TODO:     }
        object Decomposers extends Decomposer2DecomposedImplicits[Encoded] {
          import syntax._
          implicit lazy val variantDecomposer: Decomposer[Variant] = variant =>
            Decomposed(
              Keys.VariantId ~> variant.id,
              Keys.VariantName ~> variant.name
          )
        }
      }
    }

    object playjson {
      import Api.playjson._
      import play.api.libs.json._

      object JsValueKeys extends LogKeysSyntax[JsValue] with DefaultJsValueEncoders {
        val Logger: Key[Class[_]] = Key("logger").withImplicitEncoder
        val Level: Key[Level] = Key("level").withImplicitEncoder
        val Message: Key[String] = Key("msg").withImplicitEncoder
        val Timestamp: Key[LocalDateTime] = Key("ts").withExplicit(Encoder.fromToString)
        val VariantId: Key[VariantId] = Key("variantid").withExplicit(Encoder[VariantId].by(_.value))
        val VariantName: Key[String] = Key("variantname").withImplicitEncoder
        val SomeUUID: Key[UUID] = Key("uuid").withImplicitEncoder
        val RandomEncoder: Key[Random] = Key("randenc").withExplicit(Encoder[Random].by(_.nextInt(100)))
        val RandomEval: Key[Int] = Key("randeval").withImplicitEncoder
      }

      object JsonLogConfig extends Api.DefaultLogConfig[JsValue, Unit] with DefaultJsValueEncoders {
        override type Combined = JsValue
        override def combiner: EventCombiner = JsValueLogEventCombiner
        override def appender: Appender = StdOutStringLogAppender.comap(_.toString())

        override val classNameLevels: Map[String, Level] = Map(
          "Asd" -> Level.Debug
        )

        val Keys: JsValueKeys.type = JsValueKeys

        // TODO: avoid duplication of syntax and Keys definition, lookup other config to see duplication
        override def predefKeys: PredefKeys = Keys
        val syntax = ConfigSyntax(JsValueKeys, Decomposers)

        object Decomposers extends Decomposer2DecomposedImplicits[Encoded] {
          implicit lazy val variantDecomposer: Decomposer[Variant] = variant =>
            Decomposed(
              Keys.VariantId ~> variant.id,
              Keys.VariantName ~> variant.name
          )
        }
      }
    }

    object circejson {
      import Api.circejson._
      import akka.actor.{Actor, ActorPath}
      import io.circe.Json

      trait JsonKeys extends LogKeysSyntax[Json] with DefaultJsonEncoders with actor.ActorLogKeysSyntax[Json] {
        val Logger: Key[Class[_]] = Key("logger").withImplicitEncoder
        val Level: Key[Level] = Key("level").withImplicitEncoder
        val Message: Key[String] = Key("msg").withImplicitEncoder
        val Timestamp: Key[LocalDateTime] = Key("ts").withExplicit(Encoder.fromToString)
        val VariantId: Key[VariantId] = Key("variantid").withExplicit(Encoder[VariantId].by(_.value))
        val VariantName: Key[String] = Key("variantname").withImplicitEncoder
        val SomeUUID: Key[UUID] = Key("uuid").withImplicitEncoder
        val RandomEncoder: Key[Random] = Key("randenc").withExplicit(Encoder[Random].by(_.nextInt(100)))
        val RandomEval: Key[Int] = Key("randeval").withImplicitEncoder
        val ActorSource: Key[ActorPath] = Key("actorSource").withImplicitEncoder
      }
      object JsonKeys extends JsonKeys

      object JsonLogConfig extends Api.DefaultLogConfig[Json, Unit] with DefaultJsonEncoders {
        override type Combined = Json
        override def combiner: EventCombiner = JsonLogEventCombiner
        override def appender: Appender = StdOutStringLogAppender.comap(_.toString())

        override val classNameLevels: Map[String, Level] = Map(
          "Asd" -> Level.Debug
        )

        val Keys: JsonKeys.type = JsonKeys

        // TODO: avoid duplication of syntax and Keys definition, lookup other config to see duplication
        override def predefKeys: PredefKeys = Keys
        val syntax = ConfigSyntax(JsonKeys, Decomposers)

        object Decomposers extends Decomposer2DecomposedImplicits[Encoded] {
          implicit lazy val variantDecomposer: Decomposer[Variant] = variant =>
            Decomposed(
              Keys.VariantId ~> variant.id,
              Keys.VariantName ~> variant.name
          )
        }
      }

      object CatsIoJsonLogConfig extends Api.DefaultLogConfig[Json, cats.effect.IO[Unit]] with DefaultJsonEncoders {
        override type Combined = Json
        override def combiner: EventCombiner = JsonLogEventCombiner
        override def appender: Appender = CatsIoAppender.comap(_.toString())

        override val classNameLevels: Map[String, Level] = Map(
          "Asd" -> Level.Debug
        )

        val Keys: JsonKeys = JsonKeys

        // TODO: avoid duplication of syntax and Keys definition, lookup other config to see duplication
        override def predefKeys: PredefKeys = Keys
        val syntax = ConfigSyntax(JsonKeys, Decomposers)

        object Decomposers extends Decomposer2DecomposedImplicits[Encoded] with actor.ActorDecomposers[Encoded] {
          implicit lazy val variantDecomposer: Decomposer[Variant] = variant =>
            Decomposed(
              Keys.VariantId ~> variant.id,
              Keys.VariantName ~> variant.name
          )
          override implicit val actorDecomposer: Decomposers.Decomposer[Actor] = actor =>
            Decomposed(
              Keys.ActorSource -> actor.context.self.path
          )
        }
      }
    }
  }
}

object xyz {
  import Domain._
  import Main.config.syntax.decomposers._
  val composite: Main.config.Attribute = variant
}

object Main extends App {
  import Domain._
  val config = Domain.encoding.playjson.JsonLogConfig
  val config2 = Domain.encoding.circejson.CatsIoJsonLogConfig

  object Test extends config.LogInstance {
    import config.syntax._
    import config.syntax.decomposers._
    log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.Timestamp ~> LocalDateTime.MIN)
    log.event(variant)
    log.debug("test123", variant)
    log.info("test234", variant)
    log.error("test345", variant)
  }
  object Asdf extends config.LogInstance {
    import config.syntax._
    import config.syntax.decomposers._
    log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.Timestamp ~> LocalDateTime.MIN)
    log.event(variant)
    log.debug("test123", variant)
    log.info("test234", variant)
    log.error("test345", variant)
  }

  object TestEager extends config.LogInstance {
    import config.syntax._
    val rEval = new Random(0)
    val rEnc = new Random(0)
    def eval: config.Attribute = Keys.RandomEval -> rEval.nextInt(100)
    def enc: config.Attribute = Keys.RandomEncoder -> rEnc
    log.event(eval, enc)
    log.event(eval, enc)
    log.event(eval, enc)
  }

  object TestLazy extends config.LogInstance {
    import config.syntax._
    val rEval = new Random(0)
    val rEnc = new Random(0)
    val eval: config.Attribute = Keys.RandomEval ~> rEval.nextInt(100)
    val enc: config.Attribute = Keys.RandomEncoder ~> rEnc
    log.event(eval, enc)
    log.event(eval, enc)
    log.event(eval, enc)
  }

  object CatsIoTest extends config2.LogInstance {
    import config2.syntax._
    import config2.syntax.decomposers._
    val x: IO[Unit] = for {
      _ <- log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.Timestamp ~> LocalDateTime.MIN)
      _ <- log.event(variant)
      _ <- log.debug("test123", variant)
      _ <- log.info("test234", variant)
      _ <- log.error("test345", variant)
    } yield ()
    println
    println("nothing happened so far")
    x.unsafeRunSync()
    println("execution done!")
  }

  object ActorTest extends config2.LogInstance {
    import akka.actor.{Actor, ActorSystem, Props}
    import config2.syntax.decomposers._
    val as = ActorSystem("test")
    as.actorOf(Props(new SomeActor), "somesource")
    class SomeActor extends Actor {
      val x: IO[Unit] = for {
        _ <- log.event(this)
        _ <- log.event(variant)
        _ <- log.debug("test123", variant)
        _ <- log.info("test234", variant)
        _ <- log.error("test345", variant)
      } yield ()
      println
      println("nothing happened so far")
      x.unsafeRunSync()
      println("execution done!")

      context.stop(self)
      as.terminate()
      override def receive: Receive = {
        case _ =>
      }
    }
  }

  Test
  Asdf
  println
  TestEager
  TestLazy
  CatsIoTest
  ActorTest
}
