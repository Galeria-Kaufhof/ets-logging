package de.kaufhof.ets.logging

import java.util.UUID

import de.kaufhof.ets.logging.generic.LogEncoder
import de.kaufhof.ets.logging.syntax.DefaultEncoders

trait DefaultPairEncoders[E1, E2] extends DefaultEncoders[(E1, E2)] {
  def defaultEncoders1: DefaultEncoders[E1]
  def defaultEncoders2: DefaultEncoders[E2]
  type Selector[I, E] = DefaultEncoders[E] => LogEncoder[I, E]
  def encodeTuple[I](f1: Selector[I, E1], f2: Selector[I, E2])(value: I): (E1, E2) =
    (f1(defaultEncoders1).encode(value), f2(defaultEncoders2).encode(value))

  override def encodeString(string: String): (E1, E2) =
    (defaultEncoders1.encodeString(string), defaultEncoders2.encodeString(string))

  override def createIntEncoder: Encoder[Int] = encodeTuple(_.intEncoder, _.intEncoder)
  override def createLongEncoder: Encoder[Long] = encodeTuple(_.longEncoder, _.longEncoder)
  override def createFloatEncoder: Encoder[Float] = encodeTuple(_.floatEncoder, _.floatEncoder)
  override def createDoubleEncoder: Encoder[Double] = encodeTuple(_.doubleEncoder, _.doubleEncoder)
  override def createCharEncoder: Encoder[Char] = encodeTuple(_.charEncoder, _.charEncoder)
  override def createByteEncoder: Encoder[Byte] = encodeTuple(_.byteEncoder, _.byteEncoder)
  override def createUuidEncoder: Encoder[UUID] = encodeTuple(_.uuidEncoder, _.uuidEncoder)
  override def createLevelEncoder: Encoder[Level] = encodeTuple(_.levelEncoder, _.levelEncoder)
}
