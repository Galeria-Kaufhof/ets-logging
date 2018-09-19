package de.kaufhof.ets.logging.split.generic

import de.kaufhof.ets.logging.split.util._

sealed trait LogAttribute[E]
sealed trait LogPrimitive[I, E] extends LogAttribute[E] {
  def key: LogKey[I, E]
  def evaluated: I
  def encoded: E
  protected def encode: E = key.encoder.encode(evaluated)
}
case class LazyPrimitive[I, E](key: LogKey[I, E], value: Lazy[I]) extends LogPrimitive[I, E] {
  override lazy val evaluated: I = value.apply()
  override lazy val encoded: E = encode
}
case class EagerPrimitive[I, E](key: LogKey[I, E], value: I) extends LogPrimitive[I, E] {
  override val evaluated: I = value
  override val encoded: E = encode
}
case class LogDecomposed[I, E](primitives: LogPrimitive[_, E]*) extends LogAttribute[E]
