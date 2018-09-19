package de.kaufhof.ets.logging.test.encoding

import java.time.LocalDateTime
import java.util.UUID

import de.kaufhof.ets.logging._
import de.kaufhof.ets.logging.test.domain._
import de.kaufhof.ets.logging.ext.circejson._
import de.kaufhof.ets.logging.syntax._
import io.circe.Json

import scala.util.Random

object circejson {
  object JsonKeys extends LogKeysSyntax[Json] with DefaultJsonEncoders {
    val Logger: Key[Class[_]] = Key("logger").withImplicitEncoder
    val Level: Key[Level] = Key("level").withImplicitEncoder
    val Message: Key[String] = Key("msg").withImplicitEncoder
    val Timestamp: Key[LocalDateTime] = Key("ts").withExplicit(Encoder.fromToString)
    val VariantId: Key[VariantId] = Key("variantid").withExplicit(Encoder[VariantId].by(_.value))
    val VariantName: Key[String] = Key("variantname").withImplicitEncoder
    val SomeUUID: Key[UUID] = Key("uuid").withImplicitEncoder
    val RandomEncoder: Key[Random] = Key("randenc").withExplicit(Encoder[Random].by(_.nextInt(100)))
    val RandomEval: Key[Int] = Key("randeval").withImplicitEncoder
  }

  object JsonLogConfig extends DefaultLogConfig[Json, Unit] with DefaultJsonEncoders {
    override type Combined = Json
    override def combiner: EventCombiner = JsonLogEventCombiner
    override def appender: Appender = StdOutStringLogAppender.comap(_.toString())
    override val classNameLevels: Map[String, Level] = Map(
      "Asd" -> Level.Debug
    )

    val syntax = ConfigSyntax(JsonKeys, Decomposers)
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
