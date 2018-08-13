package de.kaufhof.ets.logging.stdout

import java.time.LocalDateTime
import java.util.UUID

import scala.language.implicitConversions
import scala.util.Random

object Api {
  type Lazy[A] = () => A
  object Lazy {
    def apply[A](value: => A): Lazy[A] = () => value
  }

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

  trait LogEncoder[I, O] { def encode(value: I): O }
  trait LogConfig {
    // TODO: make inner stuff top level
    type Encoded
    type Pair
    type Encoder[I] = LogEncoder[I, Encoded]
    type Combined

    protected def encodePrimitive[I](p: Primitive[I]): Pair
    protected def combineEncodedAttributes(encoded: Seq[Pair]): Combined
    protected def logCombined(combined: Combined): Unit

    protected type Prim = Primitive[_]
    protected type Prims = Seq[Primitive[_]]
    protected type PrimMap = Map[Key[_], Prim]

    type Compound[I] = LogEncoder[I, Composite[I]]

    object Encoder {
      class EncoderValueHalfApplied[A] {
        def by[B](f: A => B)(implicit derivedFrom: Encoder[B]): Encoder[A] =
          value => derivedFrom.encode(f(value))
      }
      def apply[A]: EncoderValueHalfApplied[A] = new EncoderValueHalfApplied[A]
      def fromImplicit[A: Encoder]: Encoder[A] = Predef.implicitly
      def fromToString[A]: Encoder[A] = Encoder[A].by[String](_.toString)
    }

    sealed trait Attribute
    sealed trait Primitive[I] extends Attribute {
      def key: Key[I]
      def evaluated: I
      def encoded: Encoded
      protected def encode: Encoded = key.encoder.encode(evaluated)
    }
    case class LazyPrimitive[I](key: Key[I], value: Lazy[I]) extends Primitive[I] {
      override lazy val evaluated: I = value.apply()
      override lazy val encoded: Encoded = encode
    }
    case class EagerPrimitive[I](key: Key[I], value: I) extends Primitive[I] {
      override val evaluated: I = value
      override val encoded: Encoded = encode
    }
    case class Composite[I](primitives: Primitive[_]*) extends Attribute

    case class Key[I](id: String, encoder: Encoder[I]) {
      def ->(value: I): EagerPrimitive[I] = EagerPrimitive(this, value)
      def ~>(value: => I): LazyPrimitive[I] = LazyPrimitive(this, Lazy(value))
    }

    implicit lazy val stringEncoder: Encoder[String] = encodeString
    implicit def anyCompound2composite[I: Compound](obj: I): Composite[I] = implicitly[Compound[I]].encode(obj)
    protected trait PredefKeys {
      val LoggerKey: Key[Class[_]]
      val LevelKey: Key[Level]
      val MessageKey: Key[String]
    }
    protected val predefinedKeys: PredefKeys
    import predefinedKeys._

    protected def getClassName(cls: Class[_]): String = cls.getSimpleName
    protected def classNameLevels: Map[String, Level] = Map.empty
    protected def rootLevel: Level = Level.Error
    protected def globalAttributes: Seq[Attribute]
    protected def encodeString(string: String): Encoded
    protected def convertToPrimitives(attributes: Seq[Attribute]): Seq[Primitive[_]] = {
      val zero: Prims = Seq.empty
      attributes.foldLeft(zero) { (acc, attr) =>
        attr match {
          case p: Primitive[_] => acc :+ p
          case c: Composite[_] => acc ++ c.primitives
        }
      }
    }

    protected def encodeAttributes(attributes: Seq[Attribute]): Seq[Pair] =
      convertToPrimitives(attributes).map(encodePrimitive(_))
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
    protected def forwardEvent(primMap: PrimMap): Boolean = {
      // TODO: avoid asInstanceOf if possible without shapeless
      def getPrimitive[I](key: Key[I]): Option[Primitive[I]] = primMap.get(key).map(_.asInstanceOf[Primitive[I]])
      def evaluatedPrimitive[I](key: Key[I]): Option[I] = getPrimitive(key).map(_.evaluated)
      val eventLevel: Level = evaluatedPrimitive(LevelKey).getOrElse(Level.Highest)
      val configLevel: Level = evaluatedPrimitive(LoggerKey).flatMap(configuredClassLevel).getOrElse(rootLevel)

      configLevel forwards eventLevel
    }
    protected def configuredClassLevel(cls: Class[_]): Option[Level] = {
      val name = getClassName(cls)
      findValueByKey(classNameLevels)(name.startsWith)
    }
    def findValueByKey[A, B](m: Map[A, B])(p: A => Boolean): Option[B] = m.collectFirst {
      case (pattern, value) if p(pattern) => value
    }
    def logAttributes(attributes: Seq[Attribute]): Unit = {
      val local = primsToPrimMap(convertToPrimitives(attributes))
      if (forwardEvent(local)) {
        val global = primsToPrimMap(convertToPrimitives(globalAttributes))
        val merged = mergePrimMaps(local, global)
        val prims: Prims = merged.map(_._2)(scala.collection.breakOut)
        val encoded = prims.map(encodePrimitive(_))
        logCombined(combineEncodedAttributes(encoded))
      }
    }
    protected def primsToPrimMap(prims: Prims): PrimMap = prims.map(p => (p.key, p)).toMap
    protected def mergePrimMaps(local: PrimMap, global: PrimMap): PrimMap = global ++ local

    trait Logger {
      private[Logger] def generic(attributes: Seq[Attribute]): Unit
      final def event(attrs: Attribute*): Unit = generic(attrs)
      final def debug(msg: String, attrs: Attribute*): Unit =
        generic(attrs :+ MessageKey ~> msg :+ LevelKey ~> Level.Debug)
      final def info(msg: String, attrs: Attribute*): Unit =
        generic(attrs :+ MessageKey ~> msg :+ LevelKey ~> Level.Info)
      final def error(msg: String, attrs: Attribute*): Unit =
        generic(attrs :+ MessageKey ~> msg :+ LevelKey ~> Level.Error)
    }
    object Logger {
      def apply(cls: => Class[_]): Logger = (attrs: Seq[Attribute]) => logAttributes(attrs :+ LoggerKey ~> cls)
    }
  }
}

object Domain {
  case class VariantId(value: String)
  case class Variant(id: VariantId, name: String)
  val variant: Variant = Variant(VariantId("VariantId"), "VariantName")
  val uuid: UUID = java.util.UUID.fromString("723f03f5-13a6-4e46-bdac-3c66718629df")
}

object StringLogConfig extends Api.LogConfig {
  import Domain._
  import Api._
  override type Encoded = String
  override type Pair = String
  override type Combined = String
  override def encodeString(string: String): String = string
  override def encodePrimitive[I](p: Primitive[I]): Pair = s"${p.key.id} -> ${p.encoded}"
  override def combineEncodedAttributes(encoded: Seq[Pair]): Combined = encoded.sorted.mkString(" | ")
  override def logCombined(combined: String): Unit = println(combined)
  override val predefinedKeys: PredefKeys = Keys
  override val classNameLevels: Map[String, Level] = Map(
    "Asd" -> Level.Debug
  )

  override def globalAttributes: Seq[Attribute] = Seq(
    Keys.TimestampKey ~> LocalDateTime.now()
  )

  implicit lazy val uuidEncoder: Encoder[UUID] = Encoder.fromToString
  implicit lazy val intEncoder: Encoder[Int] = Encoder.fromToString
  implicit lazy val variantCompound: Compound[Variant] = (variant) =>
    Composite(
      Keys.VariantId ~> variant.id,
      Keys.VariantName ~> variant.name
  )

  object Keys extends PredefKeys {
    val LoggerKey: Key[Class[_]] = Key[Class[_]]("logger", Encoder[Class[_]].by(getClassName))
    val LevelKey: Key[Level] = Key[Level]("level", Encoder.fromToString)
    val MessageKey: Key[String] = Key[String]("msg", Encoder.fromImplicit)
    val TimestampKey: Key[LocalDateTime] = Key[LocalDateTime]("ts", Encoder.fromToString)
    val VariantId: Key[VariantId] = Key[VariantId]("variantid", Encoder[VariantId].by(_.value))
    val VariantName: Key[String] = Key[String]("variantname", Encoder.fromImplicit)
    val SomeUUID: Key[UUID] = Key[UUID]("uuid", Encoder.fromToString)
    val RandomEncoder: Key[Random] = Key[Random]("randenc", Encoder[Random].by(_.nextInt(100)))
    val RandomEval: Key[Int] = Key[Int]("randeval", Encoder.fromToString)
  }
}

object JsonLogConfig extends Api.LogConfig {
  import Domain._
  import Api._
  import play.api.libs.json._
  import play.api.libs.json.JsValue

  override type Encoded = JsValue
  override type Pair = (String, JsValue)
  override type Combined = JsValue
  override def encodeString(string: String): Encoded = JsString(string)
  override def encodePrimitive[I](p: Primitive[I]): Pair = p.key.id -> p.encoded
  override def combineEncodedAttributes(encoded: Seq[Pair]): Combined = JsObject(encoded)
  override def logCombined(combined: Combined): Unit = println(combined.toString())
  override val predefinedKeys: PredefKeys = Keys
  override val classNameLevels: Map[String, Level] = Map(
    "Asd" -> Level.Debug
  )

  implicit class EncoderOps(e: Encoder.type) {
    def fromPlayJsonWrites[I: Writes]: Encoder[I] = implicitly[Writes[I]].writes
  }

  override def globalAttributes: Seq[Attribute] = Seq(
    Keys.TimestampKey ~> LocalDateTime.now()
  )

  implicit lazy val uuidEncoder: Encoder[UUID] = Encoder.fromToString
  implicit lazy val intEncoder: Encoder[Int] = Encoder.fromPlayJsonWrites
  implicit lazy val variantCompound: Compound[Variant] = variant =>
    Composite(
      Keys.VariantId ~> variant.id,
      Keys.VariantName ~> variant.name
    )

  object Keys extends PredefKeys {
    val LoggerKey: Key[Class[_]] = Key[Class[_]]("logger", Encoder[Class[_]].by(getClassName)(stringEncoder))
    val LevelKey: Key[Level] = Key[Level]("level", Encoder.fromToString)
    val MessageKey: Key[String] = Key[String]("msg", Encoder.fromImplicit(stringEncoder))
    val TimestampKey: Key[LocalDateTime] = Key[LocalDateTime]("ts", Encoder.fromToString)
    val VariantId: Key[VariantId] = Key[VariantId]("variantid", Encoder[VariantId].by(_.value)(stringEncoder))
    val VariantName: Key[String] = Key[String]("variantname", Encoder.fromImplicit(stringEncoder))
    val SomeUUID: Key[UUID] = Key[UUID]("uuid", Encoder.fromToString)
    val RandomEncoder: Key[Random] = Key[Random]("randenc", Encoder[Random].by(_.nextInt(100)))
    val RandomEval: Key[Int] = Key[Int]("randeval", Encoder.fromImplicit)
  }
}
object xyz {
  import Domain._
  val composite: Main.config.Attribute = variant
}

object Main extends App {
  import Domain._
  val config = JsonLogConfig
  val Keys = config.Keys

  trait LogInstance { protected lazy val log = config.Logger(getClass) }
  object Test extends LogInstance {
    log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.TimestampKey ~> LocalDateTime.MIN)
    log.event(variant)
    log.debug("test123", variant)
    log.info("test234", variant)
    log.error("test345", variant)
  }
  object Asdf extends LogInstance {
    log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.TimestampKey ~> LocalDateTime.MIN)
    log.event(variant)
    log.debug("test123", variant)
    log.info("test234", variant)
    log.error("test345", variant)
  }

  object TestEager extends LogInstance {
    val rEval = new Random(0)
    val rEnc = new Random(0)
    def eval: config.Attribute = Keys.RandomEval -> rEval.nextInt(100)
    def enc: config.Attribute = Keys.RandomEncoder -> rEnc
    log.event(eval, enc)
    log.event(eval, enc)
    log.event(eval, enc)
  }

  object TestLazy extends LogInstance {
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
