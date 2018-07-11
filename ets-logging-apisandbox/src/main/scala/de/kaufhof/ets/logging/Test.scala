package de.kaufhof.ets.logging

import java.util.{ServiceLoader, UUID}

import org.slf4j

import scala.collection.JavaConverters._
import scala.language.implicitConversions

private[logging] object tag {
  object Tag {
    def apply[U] = new Tagger[U]
  }
  class Tagger[U] {
    def apply[T](t: T): T @@ U = t.asInstanceOf[T @@ U]
  }
  trait Tagged[U]
  // helper for tagging taken from shapeless
  // into package object "tag"
  type @@[+T, U] = T with Tagged[U]
}

object Api {
  import tag._

  class LogAttribute[T](key: LogKey[T], value: => T) {
    def marker: slf4j.Marker = key.loggable.marker(key.id, value)
    def kvString: String = s"${key.id}: $value}"
  }
  type LogAttributes = Array[LogAttribute[_]]

  trait Loggable[T] {
    def marker(id: LogId[T], value: T): slf4j.Marker
  }

  object Loggable {
    class LoggableValueHalfApplied[A] {
      def by[B](f: A => B)(implicit derivedFrom: Loggable[B]): Loggable[A] =
        // fishy overwrite of tagged LogId[A] with new LogId[B]
        (id, value) => derivedFrom.marker(LogId(id), f(value))
    }
    def apply[A]: LoggableValueHalfApplied[A] = new LoggableValueHalfApplied[A]
    def fromImplicit[A: Loggable]: Loggable[A] = Predef.implicitly
  }

  trait JsonTag
  type JsonString = String @@ JsonTag
  object JsonString {
    def apply[T: JsonWriter](o: T): JsonString = implicitly[JsonWriter[T]].write(o)
  }
  trait JsonWriter[T] {
    def write(o: T): JsonString
  }

  type LogId[T] = String @@ T
  object LogId {
    def apply[T](id: String): LogId[T] = Tag[T][String](id)
  }

  case class LogKey[T](id: LogId[T], loggable: Loggable[T]) {
    def ~>(value: T): LogAttribute[T] = new LogAttribute(this, value)
    // TODO: evaluate if map method would be useful
    //   def map[B](f: T => B): LogKey[B]
    //   def mapId[B](f: LogId[T] => LogId[B]): LogKey[B] // good for key manipulation
    def to[B: Loggable]: LogKey[B] = LogKey[B](LogId[B](id), implicitly[Loggable[B]])
  }

  trait LoggableCompound[T] {
    def attributes(value: T): Array[LogAttribute[_]]
  }

  implicit def logAttribute2logAttributes(kv: LogAttribute[_]): LogAttributes = Array(kv)
  implicit def anyLoggableCompound2logAttributes[T: LoggableCompound](value: T): LogAttributes =
    implicitly[LoggableCompound[T]].attributes(value)
  // TODO: doesn't work as implicit
  implicit def logKeyA2logKeyB[A, B: Loggable](key: LogKey[A]): LogKey[B] = key.to[B]

  trait Logger {
    def error(msg: String, attrs: LogAttributes*): Unit
  }
  class DynamicMessageLogger(val log: slf4j.Logger) extends Logger {
    override def error(static: String, attrs: LogAttributes*): Unit = {
      val dynamicPart = attrs.flatten.map(_.kvString).mkString("[", ",", "]")
      log.error(s"$static :: $dynamicPart")
    }
  }
  class DynamicMessageLoggerFactory extends LoggerFactory {
    override def createLoggerFor[O](obj: O): Logger = new DynamicMessageLogger(LoggerUtils.slf4jLogFor(obj))
  }

  trait LoggerFactory {
    def createLoggerFor[O](obj: O): Logger
  }
  object LoggerFactory {
    lazy val default: LoggerFactory =
      LoggerUtils.loadSpiService[LoggerFactory](new DynamicMessageLoggerFactory)(classOf[LoggerFactory])
  }

  trait LogInstance {
    protected lazy val log: Logger = LoggerFactory.default.createLoggerFor(this)
  }

  private[logging] object LoggerUtils {
    def slf4jLogFor[O](obj: O): slf4j.Logger = slf4j.LoggerFactory.getLogger(obj.getClass)
    def loadSpiServices[S: Class]: List[S] = ServiceLoader.load[S](implicitly[Class[S]]).asScala.toList
    def loadSpiService[S: Class](fallback: => S): S = loadSpiServices.headOption.getOrElse(fallback)
    def foldMarkers(markers: Seq[slf4j.Marker], zero: slf4j.Marker): slf4j.Marker =
      markers.foldLeft(zero) { (acc, marker) =>
        // TODO: this is a workaround. why does acc.and(marker) throw a class cast exception?
        acc.add(marker)
        acc
      }
  }
}

object logstash {
  import Api._
  import net.logstash.logback.marker.Markers
  // TODO: scale out other primitive types
  implicit val jsonStringLoggable: Loggable[JsonString] = (id, value) => Markers.appendRaw(id, value)
  implicit class LogstashLoggableOps(val l: Loggable.type) extends AnyVal {
    def fromJsonWriter[A: JsonWriter]: Loggable[A] = Loggable[A].by[JsonString](implicitly[JsonWriter[A]].write(_))
  }
  implicit val stringLoggable: Loggable[String] = (id, value) => Markers.append(id, value)
  implicit val longLoggable: Loggable[Long] = (id, value) => Markers.append(id, value)
  // TODO: fishy underscore here maybe split into FieldAttribute and ObjectAttribute to separate these use cases
  implicit val mapLoggable: Loggable[Map[String, Any]] = (_, value) => Markers.appendEntries(value.asJava)
  class LogstashLogger(log: slf4j.Logger) extends Logger {
    // TODO: add possibility for lazy calls
    // TODO: scale out over severity levels
    def error(msg: String, attrs: LogAttributes*): Unit = {
      val statement = attrs.flatten.map(_.marker)
      val zero: slf4j.Marker = Markers.appendEntries(Map.empty.asJava)
      log.error(LoggerUtils.foldMarkers(statement, zero), msg)
    }
  }
  class LogstashLoggerFactory extends LoggerFactory {
    override def createLoggerFor[O](obj: O): Logger = new LogstashLogger(LoggerUtils.slf4jLogFor(obj))
  }
}

object actor extends Api.LogInstance {
  import Api._
  import akka.actor._
  import logstash._
  implicit val akkaActorContextLoggable: Loggable[ActorContext] = Loggable[ActorContext].by(_.self.path.toString)
  implicit val akkaActorLoggable: Loggable[Actor] = Loggable[Actor].by(_.context)
  implicit def akkaActorLogKey2akkaActorContextLogKey(key: LogKey[Actor])(
    implicit l: Loggable[ActorContext]
  ): LogKey[ActorContext] = key.to[ActorContext](l)

  // USAGE
  val akkaSource: LogKey[Actor] = LogKey[Actor](LogId("akkaSource"), Loggable.fromImplicit)
  implicit def actorLoggableCompound[A <: Actor]: LoggableCompound[A] = actor => Array(akkaSource ~> actor.context)
  class SomeActor extends Actor {
    log.error("test", akkaSource ~> this)
    log.error("test", akkaSource ~> this.context)
    log.error("test", this)
    context.stop(self)
    override def receive: Receive = {
      case _ =>
    }
  }
  val as = ActorSystem("test")
  as.actorOf(Props(new SomeActor), "somesource")
}

object playjson extends Api.LogInstance {
  import Api._
  import logstash._
  import play.api.libs.json._
  implicit val playJsValueJsonWriter: JsonWriter[JsValue] = jsValue => tag.Tag[JsonTag][String](Json.stringify(jsValue))
  implicit def anyPlayJsonWrites2jsonWriter[A: Writes]: JsonWriter[A] =
    a => playJsValueJsonWriter.write(implicitly[Writes[A]].writes(a))
  implicit class PlayJsonLoggableOps(l: Loggable.type) {
    def fromPlayWrites[A: Writes]: Loggable[A] = Loggable[A].by[JsonString] { a: A =>
      anyPlayJsonWrites2jsonWriter(implicitly[Writes[A]]).write(a)
    }
  }
  // TODO: better not keep experimental?
  implicit def playJsValueLogKey2playJsonWritesLogKey[A: Writes](key: LogKey[JsValue]): LogKey[A] =
    key.to(Loggable.fromPlayWrites)

  // USAGE
  case class TestClass(a: Int, b: String)
  implicit val testClassWrites: Writes[TestClass] = Json.writes[TestClass]
  val jsonString = """{"a": 123, "b": "x"}"""
  val jsValue: JsValue = Json.parse(jsonString)
  val jsKey: LogKey[JsValue] = LogKey[JsValue](LogId("json"), Loggable.fromJsonWriter)
  val objKey: LogKey[TestClass] = LogKey[TestClass](LogId("json"), Loggable.fromPlayWrites)
  log.error("logstash123", jsKey ~> jsValue)
  log.error("logstash234", jsKey ~> TestClass(234, "x"))
  log.error("logstash345", objKey ~> TestClass(345, "x"))
  //  log.error("logstash456", TestClass(456, "x"))  // TODO: implicit conversion to LoggableCompound required using shapeless
}

object circejson extends Api.LogInstance {
  import Api._
  import logstash._
  import io.circe._
  import io.circe.generic.semiauto._
  implicit val circeJsonJsonWriter: JsonWriter[Json] = json => tag.Tag[JsonTag][String](json.noSpaces)
  implicit def anyCirceJsonEncoder2jsonWriter[A: Encoder]: JsonWriter[A] =
    a => circeJsonJsonWriter.write(implicitly[Encoder[A]].apply(a))
  implicit class CirceJsonLoggableOps(l: Loggable.type) {
    def fromCirceEncoder[A: Encoder]: Loggable[A] = Loggable[A].by[JsonString] { a: A =>
      anyCirceJsonEncoder2jsonWriter(implicitly[Encoder[A]]).write(a)
    }
  }
  implicit def circeJsonLogKey2circeJsonEncoderLogKey[A: Encoder](key: LogKey[Json]): LogKey[A] =
    key.to(Loggable.fromCirceEncoder)

  // USAGE
  case class TestClass(a: Int, b: String)
  implicit val testClassEncoder: Encoder[TestClass] = deriveEncoder
  val jsonString = """{"a": 123, "b": "x"}"""
  val jsValue: Json = parser.parse(jsonString).right.get
  val jsKey: LogKey[Json] = LogKey[Json](LogId("json"), Loggable.fromCirceEncoder)
  val objKey: LogKey[TestClass] = LogKey[TestClass](LogId("json"), Loggable.fromCirceEncoder)
  log.error("circe123", jsKey ~> jsValue)
  log.error("circe234", jsKey ~> TestClass(234, "x"))
  log.error("circe345", objKey ~> TestClass(345, "x"))
}

object Main extends App with Api.LogInstance {

  import Api._
  import logstash._

  object Keys {
    val VariantId: LogKey[VariantId] = LogKey[VariantId](LogId("variantid"), Loggable[VariantId].by(_.value))
    val VariantName: LogKey[String] = LogKey[String](LogId("variantname"), Loggable.fromImplicit)
    val SomeUUID: LogKey[UUID] = LogKey[UUID](LogId("uuid"), Loggable[UUID].by(_.toString))
  }

  implicit val variantLoggableCompound: LoggableCompound[Variant] = v =>
    Array(Keys.VariantId ~> v.id, Keys.VariantName ~> v.name)

  case class VariantId(value: String)
  case class Variant(id: VariantId, name: String)
  val variant = Variant(VariantId("VariantId"), "VariantName")
  val uuid = java.util.UUID.fromString("723f03f5-13a6-4e46-bdac-3c66718629df")
  //  this.benchmark {
  log.error("someError", variant)
  log.error("someError", variant, Keys.SomeUUID ~> uuid)
  //  }
  playjson
  circejson
  actor
  actor.as.terminate()
}
