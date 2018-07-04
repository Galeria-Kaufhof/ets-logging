package de.kaufhof.ets.logging.experimental

object imports {

  //helper for tagging taken from shapeless
  object tag {
    def apply[U] = new Tagger[U]

    trait Tagged[U]
    type @@[+T, U] = T with Tagged[U]

    class Tagger[U] {
      def apply[T](t : T) : T @@ U = t.asInstanceOf[T @@ U]
    }
  }

  import tag._

  type LoggingKey[T] = String @@ T

  object LoggingKey {
    def apply[T](value: String): LoggingKey[T] = tag[T][String](value)
  }

  implicit class LoggingKeyOps[T](val key: LoggingKey[T]) extends AnyVal {
    def ~>(value: T)(implicit logFormatter: LoggableValue[T]): LoggingKeyValue = {
      tag[LoggingKeyValueTag][(LoggingKey[_], String)]((key, logFormatter.format(value)))
    }
  }

  trait LoggingKeyValueTag
  type LoggingKeyValue = (LoggingKey[_], String) @@ LoggingKeyValueTag

  trait LoggingKeyValuesTag
  type LoggingKeyValues = Array[LoggingKeyValue] @@ LoggingKeyValuesTag

  trait LoggableValue[T] {
    def format(value: T): String
  }

  trait Loggable[T] {
    def format(element: T): Array[LoggingKeyValue]
  }

  object LoggableValue {
    class LoggableValueHalfApplied[A] {
      def by[B](f: A => B)(implicit derivedFrom: LoggableValue[B]): LoggableValue[A] =
        new LoggableValue[A] {
          override def format(value: A): String = derivedFrom.format(f(value))
        }
    }

    def apply[A] = new LoggableValueHalfApplied[A]
  }

  object Loggable {
    class LoggableHalfApplied[A] {
      def by[B](f: A => B)(implicit derivedFrom: Loggable[B]): Loggable[A] =
        new Loggable[A] {
          override def format(value: A): Array[LoggingKeyValue] = derivedFrom.format(f(value))
        }
    }
  }

  trait Logger {
    def error(msg: Symbol, kvs: LoggingKeyValues*): Unit = {
      val allAttrs = kvs.flatten
      println(s"ERROR: $msg\nKeys:\n${allAttrs.map(attr => s"${attr._1} -> ${attr._2}").mkString("\n")}\n------------------------")
      println(s"IF THIS WAS A TEST - You logged successfully:\n${allAttrs.map(_._2).mkString("\n---------------\n")}\n--------------------")
    }
  }

  trait LogInstance {
    def log: Logger = new Logger {}
  }

  implicit def loggingKeyValueToValues[T](kv: LoggingKeyValue): LoggingKeyValues =
    tag[LoggingKeyValuesTag][Array[LoggingKeyValue]](Array(kv))

  implicit def anyToAttributes[T](value: T)(implicit loggable: Loggable[T]): LoggingKeyValues =
    tag[LoggingKeyValuesTag][Array[LoggingKeyValue]](loggable.format(value))
}

//User code
object UserCode extends App {
  import imports._

  case class VariantId(value: String)

  case class Variant(id: VariantId, name: String)

  object Keys {
    val VariantId = LoggingKey[VariantId]("variantid")
    val VariantName = LoggingKey[String]("variantname")
    val SomeUUID = LoggingKey[String]("uuid")
  }

  implicit val stringLoggableValue: LoggableValue[String] = new LoggableValue[String] {
    override def format(value: String) = value
  }

  implicit val variantIdLoggableValue: LoggableValue[VariantId] = LoggableValue[VariantId].by(_.value)

  implicit val variantLoggable: Loggable[Variant] = new Loggable[Variant] {
    override def format(element: Variant): Array[LoggingKeyValue] =
      Array(
        Keys.VariantId ~> element.id,
        Keys.VariantName ~> element.name
      )
  }

  val variant = Variant(VariantId("VariantId"), "VariantName")

  object Application extends LogInstance {
    def showmethelog(): Unit =
      log.error('someError, variant)
  }

  Application.showmethelog()

  object Application2 extends LogInstance {
    def showmethelog(): Unit =
      log.error('someError, variant, Keys.SomeUUID ~> "abcdefg")
  }

  Application2.showmethelog()
}
