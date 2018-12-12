package de.kaufhof.ets.logging.test.encoding

import java.time.Instant
import java.util.UUID

import akka.actor.{Actor, ActorPath}
import cats.effect.IO
import de.kaufhof.ets.logging._
import de.kaufhof.ets.logging.test.domain._
import de.kaufhof.ets.logging.ext.actor._
import de.kaufhof.ets.logging.ext.catsio.CatsIoAppender
import de.kaufhof.ets.logging.ext.circejson._
import de.kaufhof.ets.logging.generic.LogKey
import de.kaufhof.ets.logging.syntax._
import io.circe.Json

import scala.util.Random

object actor {
  object JsonKeys extends LogKeysSyntax[Json] with DefaultJsonEncoders with ActorLogKeysSyntax[Json] {
    val Logger: Key[String] = Key("logger").withImplicitEncoder
    val Level: Key[Level] = Key("level").withImplicitEncoder
    val Message: Key[String] = Key("msg").withImplicitEncoder
    val Timestamp: Key[Instant] = Key("ts").withExplicit(Encoder.fromToString)
    val VariantId: Key[VariantId] = Key("variantid").withExplicit(Encoder[VariantId].by(_.value))
    val VariantName: Key[String] = Key("variantname").withImplicitEncoder
    val SomeUUID: Key[UUID] = Key("uuid").withImplicitEncoder
    val RandomEncoder: Key[Random] = Key("randenc").withExplicit(Encoder[Random].by(_.nextInt(100)))
    val RandomEval: Key[Int] = Key("randeval").withImplicitEncoder
    val ActorSource: Key[ActorPath] = Key("actorSource").withImplicitEncoder
    val Throwable: LogKey[Throwable, Json] = Key("throwable").withImplicitEncoder
  }

  object JsonLogConfig extends DefaultLogConfig[Json, IO[Unit]] with DefaultJsonEncoders {
    override type Combined = Json
    override def combiner: EventCombiner = JsonLogEventCombiner
    override def appender: Appender = CatsIoAppender.comap(_.toString())
    override val classNameLevels: Map[String, Level] = Map(
      "Asd" -> Level.Debug
    )

    val syntax = ConfigSyntax(JsonKeys, Decomposers)
    override def predefKeys: PredefKeys = syntax.Keys

    object Decomposers extends Decomposer2DecomposedImplicits[Encoded] with ActorDecomposers[Encoded] {
      import syntax._
      implicit lazy val variantDecomposer: Decomposer[Variant] = variant =>
        Decomposed(
          Keys.VariantId ~> variant.id,
          Keys.VariantName ~> variant.name
        )
      implicit lazy val actorDecomposer: Decomposers.Decomposer[Actor] = actor =>
        Decomposed(
          Keys.ActorSource -> actor.context.self.path
        )
    }
  }
}
