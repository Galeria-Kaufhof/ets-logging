package de.kaufhof.ets.logging.generic

import de.kaufhof.ets.logging.util._

sealed trait LogAttribute[Encoded]
sealed trait LogPrimitive[I, Encoded] extends LogAttribute[Encoded] {
  def key: LogKey[I, Encoded]
  def evaluated: I
  def encoded: Encoded
  protected def encode: Encoded = key.encoder.encode(evaluated)
  def withNewKey[A](newKey: LogKey[I, A]): LogPrimitive[I, A]
  def mapEncoded[A](f: Encoded => A): LogPrimitive[I, A] = withNewKey(key.mapEncoded(_.mapEncoded(f)))
}
case class LazyPrimitive[I, Encoded](key: LogKey[I, Encoded], value: Lazy[I]) extends LogPrimitive[I, Encoded] {
  override lazy val evaluated: I = value.apply()
  override lazy val encoded: Encoded = encode
  override def withNewKey[A](newKey: LogKey[I, A]): LogPrimitive[I, A] = LazyPrimitive(newKey, value)
}
case class EagerPrimitive[I, Encoded](key: LogKey[I, Encoded], value: I) extends LogPrimitive[I, Encoded] {
  override val evaluated: I = value
  override val encoded: Encoded = encode
  override def withNewKey[A](newKey: LogKey[I, A]): LogPrimitive[I, A] = EagerPrimitive(newKey, value)
}
case class LogDecomposed[I, Encoded](primitives: LogPrimitive[_, Encoded]*) extends LogAttribute[Encoded]
