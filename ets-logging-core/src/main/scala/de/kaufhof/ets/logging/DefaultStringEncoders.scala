package de.kaufhof.ets.logging

import java.util.UUID

import de.kaufhof.ets.logging.generic.LogEncoder
import de.kaufhof.ets.logging.syntax.DefaultEncoders

trait DefaultStringEncoders extends DefaultEncoders[String] {
  override def encodeString(string: String): Encoded = string
  override def createIntEncoder: Encoder[Int] = Encoder.fromToString
  override def createLongEncoder: Encoder[Long] = Encoder.fromToString
  override def createFloatEncoder: Encoder[Float] = Encoder.fromToString
  override def createDoubleEncoder: Encoder[Double] = Encoder.fromToString
  override def createCharEncoder: Encoder[Char] = Encoder.fromToString
  override def createByteEncoder: Encoder[Byte] = Encoder.fromToString
  override def createUuidEncoder: Encoder[UUID] = Encoder.fromToString
  override def createLevelEncoder: Encoder[Level] = Encoder.fromToString
}


