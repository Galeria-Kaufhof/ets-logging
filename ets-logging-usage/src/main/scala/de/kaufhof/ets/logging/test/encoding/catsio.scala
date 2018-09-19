package de.kaufhof.ets.logging.test.encoding

import cats.effect.IO
import de.kaufhof.ets.logging._
import de.kaufhof.ets.logging.test.domain._
import de.kaufhof.ets.logging.ext.catsio._
import de.kaufhof.ets.logging.ext.circejson._
import de.kaufhof.ets.logging.syntax._
import io.circe.Json

object catsio {
  object CatsIoJsonLogConfig extends DefaultLogConfig[Json, IO[Unit]] with DefaultJsonEncoders {
    override type Combined = Json
    override def combiner: EventCombiner = JsonLogEventCombiner
    override def appender: Appender = CatsIoAppender.comap(_.toString())
    override val classNameLevels: Map[String, Level] = Map(
      "Asd" -> Level.Debug
    )

    val syntax = ConfigSyntax(circejson.JsonKeys, Decomposers)
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
