package de.kaufhof.ets.logging.test.encoding

import java.time.Instant
import java.util.UUID

import de.kaufhof.ets.logging._
import de.kaufhof.ets.logging.test.domain._
import de.kaufhof.ets.logging.syntax._

import scala.util.Random

object string {
  object StringKeys extends LogKeysSyntax[String] with DefaultStringEncoders {
    val Logger: Key[String] = Key("logger").withImplicitEncoder
    val Level: Key[Level] = Key("level").withImplicitEncoder
    val Message: Key[String] = Key("msg").withImplicitEncoder
    val Timestamp: Key[Instant] = Key("ts").withExplicit(Encoder.fromToString)
    val VariantId: Key[VariantId] = Key("variantid").withExplicit(Encoder[VariantId].by(_.value))
    val VariantName: Key[String] = Key("variantname").withImplicitEncoder
    val SomeUUID: Key[UUID] = Key("uuid").withImplicitEncoder
    val RandomEncoder: Key[Random] = Key("randenc").withExplicit(Encoder[Random].by(_.nextInt(100)))
    val RandomEval: Key[Int] = Key("randeval").withImplicitEncoder
    val Throwable: Key[Throwable] = Key("throwable").withImplicitEncoder
  }

  object StringLogConfig extends DefaultLogConfig[String, Unit] with DefaultStringEncoders {
    override type Combined = String
    override def combiner: LogEventCombiner[String, String] = StringLogEventCombiner
    override def appender: Appender = StdOutStringLogAppender
    override val classNameLevels: Map[String, Level] = Map(
      "Asd" -> Level.Debug
    )

    val syntax = ConfigSyntax(StringKeys, Decomposers)
    override def predefKeys: PredefKeys = syntax.Keys

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
