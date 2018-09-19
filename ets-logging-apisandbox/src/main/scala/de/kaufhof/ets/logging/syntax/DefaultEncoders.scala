package de.kaufhof.ets.logging.syntax

import java.util.UUID

import de.kaufhof.ets.logging.{ClassNameExtractor, Level}

trait DefaultEncoders[E] extends LogTypeDefinitions[E] with LogEncoderSyntax[E] with ClassNameExtractor {
  def createIntEncoder: Encoder[Int]
  def createLongEncoder: Encoder[Long]
  def createFloatEncoder: Encoder[Float]
  def createDoubleEncoder: Encoder[Double]
  def createCharEncoder: Encoder[Char]
  def createByteEncoder: Encoder[Byte]
  def createUuidEncoder: Encoder[UUID]
  def createLevelEncoder: Encoder[Level]
  def createClassEncoder: Encoder[Class[_]] = Encoder[Class[_]].by(getClassName)

  implicit lazy val intEncoder: Encoder[Int] = createIntEncoder
  implicit lazy val longEncoder: Encoder[Long] = createLongEncoder
  implicit lazy val floatEncoder: Encoder[Float] = createFloatEncoder
  implicit lazy val doubleEncoder: Encoder[Double] = createDoubleEncoder
  implicit lazy val charEncoder: Encoder[Char] = createCharEncoder
  implicit lazy val byteEncoder: Encoder[Byte] = createByteEncoder
  implicit lazy val uuidEncoder: Encoder[UUID] = createUuidEncoder
  implicit lazy val levelEncoder: Encoder[Level] = createLevelEncoder
  implicit lazy val classEncoder: Encoder[Class[_]] = createClassEncoder
}
