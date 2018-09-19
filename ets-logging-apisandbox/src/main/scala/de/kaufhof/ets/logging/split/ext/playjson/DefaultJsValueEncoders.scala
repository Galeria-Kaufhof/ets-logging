package de.kaufhof.ets.logging.split.ext.playjson

import java.util.UUID

import de.kaufhof.ets.logging.split.Level
import de.kaufhof.ets.logging.split.syntax.DefaultEncoders
import play.api.libs.json._

trait DefaultJsValueEncoders extends DefaultEncoders[JsValue] {
  implicit class EncoderOps(e: Encoder.type) {
    def fromPlayJsonWrites[I](implicit w: Writes[I]): Encoder[I] = w.writes
  }
  override def encodeString(string: String): Encoded = JsString(string)

  override def createIntEncoder: Encoder[Int] = Encoder.fromPlayJsonWrites
  override def createLongEncoder: Encoder[Long] = Encoder.fromPlayJsonWrites
  override def createFloatEncoder: Encoder[Float] = Encoder.fromPlayJsonWrites
  override def createDoubleEncoder: Encoder[Double] = Encoder.fromPlayJsonWrites
  override def createCharEncoder: Encoder[Char] = Encoder[Char].by(_.toByte)
  override def createByteEncoder: Encoder[Byte] = Encoder.fromPlayJsonWrites
  override def createUuidEncoder: Encoder[UUID] = Encoder.fromPlayJsonWrites
  override def createLevelEncoder: Encoder[Level] = Encoder.fromToString
}
