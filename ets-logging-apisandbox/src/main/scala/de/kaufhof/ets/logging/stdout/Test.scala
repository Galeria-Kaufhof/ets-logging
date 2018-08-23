package de.kaufhof.ets.logging.stdout

import java.time.LocalDateTime
import java.util.UUID

import de.kaufhof.ets.logging.stdout.Api._
import de.kaufhof.ets.logging.stdout.StringLogConfig.Encoder
import net.logstash.logback.marker.Markers
import play.api.libs.json._
import org.slf4j
import org.slf4j.Marker

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
  object SdtoutStringLogAppender extends LogAppender[String, Unit] {
    override def append(combined: String): Unit = println(combined)
    override def ignore: Unit = ()
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
    def debug(msg: String, attrs: LogAttribute[E]*): O
    def info(msg: String, attrs: LogAttribute[E]*): O
    def error(msg: String, attrs: LogAttribute[E]*): O
  }

  type LogEvent[E] = Map[LogKey[_, E], LogPrimitive[_, E]]

  trait LogEventCombiner[E, C] extends LogTypeDefinitions[E] with LogCombinedTypeDefinition[C]{
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
  object JsValueLogEventCombiner extends PairLogEventCombiner[JsValue, JsValue] {
    override type Pair = (String, JsValue)
    override protected def primToPair[I](p: Primitive[I]): Pair = p.key.id -> p.encoded
    override protected def combinePairs(encoded: Seq[Pair]): Combined = JsObject(encoded)
  }
  // TODO: provide SLF4J based event filter taking the underlying configuration into account
  object Slf4jMarkerLogEventCombiner extends LogEventCombiner[slf4j.Marker, slf4j.Marker] {
    override def combine(e: LogEvent[Encoded]): Marker = {
      import scala.collection.JavaConverters._
      val zero: slf4j.Marker = Markers.appendEntries(Map.empty.asJava)
      e.foldLeft(zero) { (acc, marker) =>
        // TODO: this is a workaround. why does acc.and(marker) throw a class cast exception?
        acc.add(marker._2.encoded)
        acc
      }
    }
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
  trait ClassLevelLogEventFilter[E] extends LogEventFilter[E] with ClassNameExtractor with PredefKeysInstance[E] with LogTypeDefinitions[E] {
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
  trait DefaultAttributeGatherer[E] extends LogTypeDefinitions[E] with LogAttributeGatherer[E] with PredefKeysInstance[E] {
    override def gatherGlobal: Seq[Attribute] = Seq(
      predefKeys.Timestamp ~> LocalDateTime.now()
    )
  }

  trait LogEventOps[E] extends LogTypeDefinitions[E] {
    private def primsToEvent(prims: Prims): Event = prims.map(p => (p.key, p)).toMap
    def fromAttributes(attributes: Seq[Attribute]): Event = primsToEvent(attributesToPrims(attributes))
    def aggregate(acc:Event, next: Event): Event = acc ++ next
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

  trait LogKeySyntax[E] extends LogTypeDefinitions[E]{

    object Key {
      def apply[I](id: String, encoder: Encoder[I]): Key[I] = LogKey(id, encoder)
    }
    object Decomposed {
      def apply[I](primitives: Primitive[_]*): Decomposed[I] = LogDecomposed(primitives :_*)
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

  trait DefaultLoggerFactory[E, O] extends LoggerFactory[E, O] with PredefKeysInstance[E] with LogAttributeProcessor[E, O] {
    def createLogger(cls: Class[_]): Logger = new Logger {
      private val clsAttr = predefKeys.Logger -> cls
      private def generic(attributes: Seq[LogAttribute[E]]): Output = process(attributes :+ clsAttr)
      final def event(attrs: LogAttribute[E]*): Output = generic(attrs)
      final def debug(msg: String, attrs: LogAttribute[E]*): Output =
        generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Debug)
      final def info(msg: String, attrs: LogAttribute[E]*): Output =
        generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Info)
      final def error(msg: String, attrs: LogAttribute[E]*): Output =
        generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Error)
    }
  }

  trait DefaultEncoders[E] extends LogTypeDefinitions[E] with LogEncoderSyntax[E] {
    def createUuidEncoder: Encoder[UUID]
    def createIntEncoder: Encoder[Int]

    implicit lazy val uuidEncoder: Encoder[UUID] = createUuidEncoder
    implicit lazy val intEncoder: Encoder[Int] = createIntEncoder
  }

  trait DefaultStringEncoders extends DefaultEncoders[String] {
    override def createUuidEncoder: Encoder[UUID] = Encoder.fromToString
    override def createIntEncoder: Encoder[Int] = Encoder.fromToString
    override def encodeString(string: String): Encoded = string
  }

  trait JsValueStringEncoders extends DefaultEncoders[JsValue] {
    implicit class EncoderOps(e: Encoder.type) {
      def fromPlayJsonWrites[I: Writes]: Encoder[I] = implicitly[Writes[I]].writes
    }
    override def createUuidEncoder: Encoder[UUID] = Encoder.fromToString
    override def createIntEncoder: Encoder[Int] = Encoder.fromPlayJsonWrites
    override def encodeString(string: String): Encoded = JsString(string)
  }

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

}

object Domain {
  case class VariantId(value: String)
  case class Variant(id: VariantId, name: String)
  val variant: Variant = Variant(VariantId("VariantId"), "VariantName")
  val uuid: UUID = java.util.UUID.fromString("723f03f5-13a6-4e46-bdac-3c66718629df")
}

object StringLogConfig extends Api.DefaultLogConfig[String, Unit] with DefaultStringEncoders {
  // TODO: try to avoid self type here
  self =>

  override type Combined = String
  override def combiner: LogEventCombiner[String, String] = StringLogEventCombiner
  override def appender: Appender = SdtoutStringLogAppender
  override val classNameLevels: Map[String, Level] = Map(
    "Asd" -> Level.Debug
  )

  object Keys extends PredefKeys {
    import Domain._
    val Logger: Key[Class[_]] = Key[Class[_]]("logger", Encoder[Class[_]].by(getClassName))
    val Level: Key[Level] = Key[Level]("level", Encoder.fromToString)
    val Message: Key[String] = Key[String]("msg", Encoder.fromImplicit)
    val Timestamp: Key[LocalDateTime] = Key[LocalDateTime]("ts", Encoder.fromToString)
    val VariantId: Key[VariantId] = Key[VariantId]("variantid", Encoder[VariantId].by(_.value))
    val VariantName: Key[String] = Key[String]("variantname", Encoder.fromImplicit)
    val SomeUUID: Key[UUID] = Key[UUID]("uuid", Encoder.fromToString)
    val RandomEncoder: Key[Random] = Key[Random]("randenc", Encoder[Random].by(_.nextInt(100)))
    val RandomEval: Key[Int] = Key[Int]("randeval", Encoder.fromToString)
  }

  // TODO: avoid duplication of syntax and Keys definition, lookup other config to see duplication
  // TODO: it is not so easy my first attempt resulted in intellij being confused about the Keys object
  // TODO: or it could not find the general decomposer implicits being mixed in
  override def predefKeys: PredefKeys = Keys
  object syntax extends Decomposers with Decomposer2DecomposedImplicits[Encoded] {
    val Keys: self.Keys.type = self.Keys
  }

  // TODO: here is a chance to reduce duplication, any decomposer depends on the domain, its keys and the encoding
  // TODO: the encoding isn't exactly specific to the decomposers, it should be possible to define decomposers
  // TODO: according to only knowing the keys relative to an encoded type E
  // TODO: maybe we just need another type K for the Keys the user is going to provide like this:
  // TODO:     trait LogDecomposers[K] {
  // TODO:       type Keys = K
  // TODO:       val Keys: Keys
  // TODO:     }
  trait Decomposers {
    import Domain._
    implicit lazy val variantDecomposer: Decomposer[Variant] = variant => Decomposed(
      Keys.VariantId ~> variant.id,
      Keys.VariantName ~> variant.name
    )
  }
}


object JsonLogConfig extends Api.DefaultLogConfig[JsValue, Unit] with JsValueStringEncoders {
  self =>

  override type Combined = JsValue
  override def combiner: EventCombiner = JsValueLogEventCombiner
  override def appender: Appender = SdtoutStringLogAppender.comap(_.toString())
  override def encodeString(string: String): Encoded = JsString(string)

  override val classNameLevels: Map[String, Level] = Map(
    "Asd" -> Level.Debug
  )

  object Keys extends PredefKeys {
    import Domain._
    val Logger: Key[Class[_]] = Key[Class[_]]("logger", Encoder[Class[_]].by(getClassName)(stringEncoder))
    val Level: Key[Level] = Key[Level]("level", Encoder.fromToString)
    val Message: Key[String] = Key[String]("msg", Encoder.fromImplicit(stringEncoder))
    val Timestamp: Key[LocalDateTime] = Key[LocalDateTime]("ts", Encoder.fromToString)
    val VariantId: Key[VariantId] = Key[VariantId]("variantid", Encoder[VariantId].by(_.value)(stringEncoder))
    val VariantName: Key[String] = Key[String]("variantname", Encoder.fromImplicit(stringEncoder))
    val SomeUUID: Key[UUID] = Key[UUID]("uuid", Encoder.fromToString)
    val RandomEncoder: Key[Random] = Key[Random]("randenc", Encoder[Random].by(_.nextInt(100)))
    val RandomEval: Key[Int] = Key[Int]("randeval", Encoder.fromImplicit)
  }

  // TODO: avoid duplication of syntax and Keys definition, lookup other config to see duplication
  override def predefKeys: PredefKeys = Keys
  object syntax extends Decomposers with Decomposer2DecomposedImplicits[Encoded] {
    val Keys: self.Keys.type = self.Keys
  }

  trait Decomposers {
    import Domain._
    implicit lazy val variantDecomposer: Decomposer[Variant] = variant => Decomposed(
      Keys.VariantId ~> variant.id,
      Keys.VariantName ~> variant.name
    )
  }
}

object xyz {
  import Domain._
  import Main.config.syntax._
  val composite: Main.config.Attribute = variant
}

object Main extends App {
  import Domain._
  val config = JsonLogConfig
  import config.syntax._

  object Test extends config.LogInstance {
    log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.Timestamp ~> LocalDateTime.MIN)
    log.event(variant)
    log.debug("test123", variant)
    log.info("test234", variant)
    log.error("test345", variant)
  }
  object Asdf extends config.LogInstance {
    log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.Timestamp ~> LocalDateTime.MIN)
    log.event(variant)
    log.debug("test123", variant)
    log.info("test234", variant)
    log.error("test345", variant)
  }

  object TestEager extends config.LogInstance {
    val rEval = new Random(0)
    val rEnc = new Random(0)
    def eval: config.Attribute = Keys.RandomEval -> rEval.nextInt(100)
    def enc: config.Attribute = Keys.RandomEncoder -> rEnc
    log.event(eval, enc)
    log.event(eval, enc)
    log.event(eval, enc)
  }

  object TestLazy extends config.LogInstance {
    val rEval = new Random(0)
    val rEnc = new Random(0)
    val eval: config.Attribute = Keys.RandomEval ~> rEval.nextInt(100)
    val enc: config.Attribute = Keys.RandomEncoder ~> rEnc
    log.event(eval, enc)
    log.event(eval, enc)
    log.event(eval, enc)
  }

  Test
  Asdf
  println
  TestEager
  TestLazy
}
