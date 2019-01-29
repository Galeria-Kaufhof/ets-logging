package de.kaufhof.ets.logging.test.encoding

import java.time.Instant
import java.util

import de.kaufhof.ets.logging._
import de.kaufhof.ets.logging.ext.slf4j.EtsSlf4jSpiProvider
import de.kaufhof.ets.logging.ext.slf4j.EtsSlf4jSpiProvider.LogAttributeResolver
import de.kaufhof.ets.logging.syntax._
import de.kaufhof.ets.logging.test.encoding.slf4j.{StringKeys, StringLogConfig, TestMarker, TestMarker2}
import org.slf4j.Marker

class Slf4jProvider extends EtsSlf4jSpiProvider[String, Unit] {
  override def config: DefaultLogConfig[String, Unit] = StringLogConfig
  override def logMarker: LogAttributeResolver[String] = {
    case m: TestMarker => StringKeys.TestMarker -> m
    case m: TestMarker2 => StringKeys.TestMarker2 -> m
  }
}

object slf4j {
  object StringKeys extends LogKeysSyntax[String] with DefaultStringEncoders {
    val Logger: Key[String] = Key("logger").withImplicitEncoder
    val Level: Key[Level] = Key("level").withImplicitEncoder
    val Message: Key[String] = Key("msg").withImplicitEncoder
    val Timestamp: Key[Instant] = Key("ts").withExplicit(Encoder.fromToString)
    val Throwable: Key[Throwable] = Key("throwable").withImplicitEncoder
    val TestMarker: Key[TestMarker] = Key("testmarker").withExplicit((value: TestMarker) => value.value)
    val TestMarker2: Key[TestMarker2] = Key("testmarker").withExplicit((value: TestMarker2) => value.value)
  }

  object StringLogConfig extends DefaultLogConfig[String, Unit] with DefaultStringEncoders {
    override type Combined = String
    override def combiner: LogEventCombiner[String, String] = StringLogEventCombiner
    override def appender: Appender = StdOutStringLogAppender
    override val classNameLevels: Map[String, Level] = Map(
      "Asd" -> Level.Debug
    )

    override def predefKeys: PredefKeys = StringKeys
  }

  class TestMarker(val value: String) extends Marker {
    override def getName: String = "test-marker"
    override def add(reference: Marker): Unit = ()
    override def remove(reference: Marker): Boolean = false
    override def hasChildren: Boolean = false
    override def hasReferences: Boolean = false
    override def iterator(): util.Iterator[Marker] = new util.ArrayList[Marker]().iterator()
    override def contains(other: Marker): Boolean = false
    override def contains(name: String): Boolean = false
  }

  class TestMarker2 extends TestMarker("asdf")
}
