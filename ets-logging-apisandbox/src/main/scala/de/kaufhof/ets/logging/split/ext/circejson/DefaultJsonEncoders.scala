package de.kaufhof.ets.logging.split.ext.circejson

import java.util.UUID

import de.kaufhof.ets.logging.split.Level
import de.kaufhof.ets.logging.split.syntax.DefaultEncoders
import io.circe
import io.circe.Json

trait DefaultJsonEncoders extends DefaultEncoders[Json] {
  implicit class EncoderOps(e: Encoder.type) {
    def fromCirceJsonEncoder[I](implicit w: circe.Encoder[I]): Encoder[I] = w.apply
  }
  override def encodeString(string: String): Encoded = Json.fromString(string)

  override def createIntEncoder: Encoder[Int] = Encoder.fromCirceJsonEncoder
  override def createLongEncoder: Encoder[Long] = Encoder.fromCirceJsonEncoder
  override def createFloatEncoder: Encoder[Float] = Encoder.fromCirceJsonEncoder
  override def createDoubleEncoder: Encoder[Double] = Encoder.fromCirceJsonEncoder
  override def createCharEncoder: Encoder[Char] = Encoder[Char].by(_.toByte)
  override def createByteEncoder: Encoder[Byte] = Encoder.fromCirceJsonEncoder
  override def createUuidEncoder: Encoder[UUID] = Encoder.fromCirceJsonEncoder
  override def createLevelEncoder: Encoder[Level] = Encoder.fromToString
}
