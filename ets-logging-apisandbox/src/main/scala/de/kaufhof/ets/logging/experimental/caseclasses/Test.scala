package de.kaufhof.ets.logging.experimental.caseclasses

import de.kaufhof.ets.logging.experimental.Benchmark._

import net.logstash.logback.marker.MapEntriesAppendingMarker
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.language.implicitConversions

object Api {
  trait LoggableValue[T] {
    def format(value: T): String
  }

  trait LoggableKeysValues[T] {
    def format(element: T): List[Attribute[_]]
  }

  case class Attribute[T](key: AttributeKey, value: T)(implicit loggableValue: LoggableValue[T]) {
    def format: (String, String) = key.name -> loggableValue.format(value)
  }

  case class Attributes(attrs: List[Attribute[_]]) {
    def ++(other: Attributes) = Attributes(attrs ++ other.attrs)
  }

  case class AttributeKey(name: String) {
    def ~>[A](value: A)(implicit logFormatter: LoggableValue[A]): Attribute[A] =
      Attribute(this, value)(logFormatter)
  }

  class Logger(log: org.slf4j.Logger) {
    def error(msg: String, kvs: Attributes*): Unit = {
      val attributes = kvs.flatMap(_.attrs)
      val map: Map[String, String] = attributes.map(_.format).toMap
      val marker = new MapEntriesAppendingMarker(map.asJava)
      log.error(marker, msg)
    }
  }

  trait LogInstance {
    def log: Logger = new Logger(LoggerFactory.getLogger(getClass))
  }

  implicit def attributeToAttributes[T](attribute: Attribute[T]): Attributes =
    Attributes(List(attribute))

  implicit def anyToAttributes[T: LoggableKeysValues](value: T): Attributes =
    Attributes(implicitly[LoggableKeysValues[T]].format(value))
}

object Main extends App with Api.LogInstance {

  import Api._

  case class Variant(id: String, name: String)
  val variant = Variant("VariantId", "VariantName")

  object Keys {
    val VariantId = AttributeKey("variantid")
    val VariantName = AttributeKey("variantname")
    val SomeUUID = AttributeKey("uuid")
  }

  implicit val stringLoggableValue: LoggableValue[String] =
    new LoggableValue[String] {
      override def format(value: String) = value
    }

  implicit val variantLoggable: LoggableKeysValues[Variant] =
    new LoggableKeysValues[Variant] {
      override def format(element: Variant): List[Attribute[_]] =
        List(
          Keys.VariantId ~> element.id,
          Keys.VariantName ~> element.name
        )
    }

  this.benchmark {
    log.error("someError", variant)
    log.error("someError", variant, Keys.SomeUUID ~> "abcdefg")
  }
}
