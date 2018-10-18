package de.kaufhof.ets.logging.ext.slf4j

import java.time.Instant
import java.util

import de.kaufhof.ets.logging._
import de.kaufhof.ets.logging.ext.slf4j.EtsSlf4jSpiProvider.LogAttributeResolver
import de.kaufhof.ets.logging.syntax.LogKeysSyntax
import org.slf4j.Marker

class Slf4jProvider extends EtsSlf4jSpiProvider[String, Unit] {
  override def config: DefaultLogConfig[String, Unit] = StringLogConfig
  override def logMarker: LogAttributeResolver[String] = {
    case m: ExtendedTestMarker => StringKeys.ExtendedTestMarker -> m
    case m: TestMarker => StringKeys.TestMarker -> m
  }
}

object StringKeys extends LogKeysSyntax[String] with DefaultStringEncoders {
  val Logger: Key[String] = Key("logger").withImplicitEncoder
  val Level: Key[Level] = Key("level").withImplicitEncoder
  val Message: Key[String] = Key("msg").withImplicitEncoder
  val Timestamp: Key[Instant] = Key("ts").withExplicit(new Encoder[Instant] {
    override def encode(value: Instant): Encoded = "2018-12-06T08:14:21.770Z"
  })
  val Throwable: Key[Throwable] = Key("throwable").withExplicit(new Encoder[Throwable] {
    override def encode(value: Throwable): Encoded = value.getClass.getName
  })
  val TestMarker: Key[TestMarker] = Key("testmarker").withExplicit(new Encoder[TestMarker] {
    override def encode(value: TestMarker): Encoded = value.name
  })
  val ExtendedTestMarker: Key[ExtendedTestMarker] = Key("extended-testmarker").withExplicit(new Encoder[ExtendedTestMarker] {
    override def encode(value: ExtendedTestMarker): Encoded = value.name
  })
}

object StringLogConfig extends DefaultLogConfig[String, Unit] with DefaultStringEncoders {
  override type Combined = String
  override def combiner: LogEventCombiner[String, String] = StringLogEventCombiner
  override def appender: Appender = RecordingAppender
  override def predefKeys: PredefKeys = StringKeys
}

class TestMarker(val name: String) extends Marker {
  override def getName: String = name

  override def add(reference: Marker): Unit = ()

  override def remove(reference: Marker): Boolean = false

  override def hasChildren: Boolean = false

  override def hasReferences: Boolean = false

  override def iterator(): util.Iterator[Marker] = new util.ArrayList[Marker]().iterator()

  override def contains(other: Marker): Boolean = false

  override def contains(name: String): Boolean = false
}

class ExtendedTestMarker extends TestMarker("extended-test-marker")

object RecordingAppender extends LogAppender[String, Unit] {

  private val list = new scala.collection.mutable.ListBuffer[String]

  def records: Seq[String] = list

  def clear(): Unit = list.clear()

  override def append(combined: Combined): Unit = list += combined

  override def ignore: Unit = ()
}
