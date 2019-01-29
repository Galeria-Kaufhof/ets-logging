package de.kaufhof.ets.logging.generic

import de.kaufhof.ets.logging.util.Lazy

case class LogKey[I, Encoded](id: String, encoder: LogEncoder[I, Encoded]) {
  def ->(value: I): EagerPrimitive[I, Encoded] = EagerPrimitive(this, value)
  def ~>(value: => I): LazyPrimitive[I, Encoded] = LazyPrimitive(this, Lazy(value))

  def mapEncoded[A](f: LogEncoder[I, Encoded] => LogEncoder[I, A]): LogKey[I, A] = LogKey(id, f(encoder))
}
