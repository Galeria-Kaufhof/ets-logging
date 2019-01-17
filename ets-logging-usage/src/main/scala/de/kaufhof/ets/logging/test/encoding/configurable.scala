package de.kaufhof.ets.logging.test.encoding

import java.time.Instant

import de.kaufhof.ets.logging._
import de.kaufhof.ets.logging.ext.circejson.{DefaultJsonEncoders, JsonLogEventCombiner, StdOutJsonLogAppender}
import de.kaufhof.ets.logging.generic.LogEvent
import de.kaufhof.ets.logging.syntax.{ConfigSyntax, Decomposer2DecomposedImplicits, DefaultEncoders, LogKeysSyntax}
import io.circe.Json

object configurable {
  object TupledKeys extends LogKeysSyntax[(String, Json)] with DefaultPairEncoders[String, Json] {
    override def defaultEncoders1: DefaultEncoders[String] = new DefaultStringEncoders {}
    override def defaultEncoders2: DefaultEncoders[Json] = new DefaultJsonEncoders {}

    override val Logger: Key[String] = Key("logger").withImplicitEncoder
    override val Level: Key[Level] = Key("level").withImplicitEncoder
    override val Message: Key[String] = Key("message").withImplicitEncoder
    override val Throwable: Key[Throwable] = Key("throwable").withImplicitEncoder
    override val Timestamp: Key[Instant] = Key("timestamp").withExplicit(Encoder.fromToString)
  }

  object TupledLogConfig extends DefaultLogConfig[(String, Json), Unit] with DefaultPairEncoders[String, Json] {
    override type Combined = Either[String, Json]
    override def combiner: EventCombiner = new TupleToEitherCombiner[String, Json] {
      // setup logic to either take left or right
      override def takeLeft(e: LogEvent[(String, Json)]): Boolean = true

      override def combiner1: LogEventCombiner[String, String] = StringLogEventCombiner
      override def combiner2: LogEventCombiner[Json, Json] = JsonLogEventCombiner
    }

    override def appender: Appender = new Appender {
      override def append(combined: Combined): TupledLogConfig.Output =
        combined.fold(StdOutStringLogAppender.append, StdOutJsonLogAppender.append)

      override def ignore: TupledLogConfig.Output = ()
    }

    override val classNameLevels: Map[String, Level] = Map(
      "Asd" -> Level.Debug
    )

    val syntax = ConfigSyntax(TupledKeys, Decomposers)
    override def predefKeys: PredefKeys = syntax.Keys

    object Decomposers extends Decomposer2DecomposedImplicits[Encoded]

    override def defaultEncoders1: DefaultEncoders[String] = new DefaultStringEncoders {}
    override def defaultEncoders2: DefaultEncoders[Json] = new DefaultJsonEncoders {}
  }
}
