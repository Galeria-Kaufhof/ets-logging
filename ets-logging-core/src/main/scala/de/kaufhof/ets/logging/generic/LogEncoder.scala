package de.kaufhof.ets.logging.generic

trait LogEncoder[-I, Encoded] {
  outer =>
  def encode(value: I): Encoded
  def map[A](f: Encoded => A): LogEncoder[I, A] = (value: I) => f(outer.encode(value))
}
