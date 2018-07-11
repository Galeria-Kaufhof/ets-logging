package de.kaufhof.ets.logging.experimental.loggablekey

import net.logstash.logback.marker.Markers
import org.slf4j

import scala.annotation.implicitNotFound
import scala.collection.JavaConverters._
import scala.language.implicitConversions

object Api {
  object tag {
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

  import tag._

  case class LogAttribute[T](key: LoggableKey[T], value: T) {
    def marker = key.loggable.mark(key.key, value)
  }
  type LogAttributes = Array[LogAttribute[_]]

  trait LoggableValue[T] {
    def format(value: T): String
  }
  object LoggableValue {
    class LoggableValueHalfApplied[A] {
      def by[B](f: A => B)(implicit derivedFrom: LoggableValue[B]): LoggableValue[A] =
        value => derivedFrom.format(f(value))
    }
    def apply[A] = new LoggableValueHalfApplied[A]
  }

  type LogKey[T] = String @@ T
  object LogKey {
    def apply[T](key: String): LogKey[T] = Tag[T][String](key)
  }
  implicit class LogKeyOps[T](val logKey: LogKey[T]) extends AnyVal {
    def indexDefault(implicit loggable: Loggable[T]): LoggableKey[T] = LoggableKey(logKey, loggable)
  }

  case class LoggableKey[T](key: LogKey[T], loggable: Loggable[T]) {
    def ~>(value: T): LogAttribute[T] = LogAttribute(this, value)
  }
  object LoggableKey {
    def fromDefault[T: Loggable](key: String) = LoggableKey(LogKey[T](key), implicitly[Loggable[T]])
    def fromFunction[T](key: String)(f: (LogKey[T], T) => slf4j.Marker) = LoggableKey(
      LogKey[T](key),
      (key: LogKey[T], value: T) => f(key, value)
    )
  }

  @implicitNotFound(
    "No implicit Loggable in scope for ${T}\n" +
      " import one if available or previously define one like this:\n" +
      " implicit val variant: LoggableKey[${T}] = ???"
  )
  trait Loggable[T] {
    def mark(key: LogKey[T], value: T): slf4j.Marker
  }

  implicit val stringLoggable: Loggable[String] = (key, value) => Markers.append(key, value)
  implicit val longLoggable: Loggable[Long] = (key, value) => Markers.append(key, value)
  implicit val mapLoggable: Loggable[Map[String, Any]] = (_, value) => Markers.appendEntries(value.asJava)

  implicit def logAttribute2logAttributes(kv: LogAttribute[_]): LogAttributes = Array(kv)
  implicit def any2logAttributes[T](value: T)(implicit loggable: LoggableKey[T]): LogAttributes =
    Array(loggable ~> value)

  case class Logger(log: slf4j.Logger) {
    def error(msg: String, attrs: LogAttributes*): Unit = {
      val markers = attrs.flatten.map(_.marker)
      val maybeMarker = markers.reduceLeftOption { (a, b) =>
        a.add(b)
        a
      }
      println("asdf" + markers)
      maybeMarker match {
        case Some(marker) => log.error(marker, msg)
        case None         => log.error(msg)
      }
    }
  }

  trait LogInstance {
    protected val log = Logger(slf4j.LoggerFactory.getLogger(getClass))
  }
}

object Main extends App with Api.LogInstance {

  import Api._
  import Keys._

  object Keys {
    implicit val variant2: LoggableKey[Variant] = LoggableKey[Variant](
      LogKey("variant"),
      (key, value) => ???
    )
    implicit val variantId: LoggableKey[VariantId] = LoggableKey.fromFunction[VariantId]("variantid")( (key,id) =>
      Markers.append(key, id.value)
    )
    implicit val variantName: LoggableKey[String] = LoggableKey.fromDefault[String]("variantname")
  }

  case class VariantId(value: String)
  case class Variant(id: VariantId, name: String)
  val variant = Variant(VariantId("VariantId"), "VariantName")

  log.error("someError", Api.any2logAttributes(variant))
}
