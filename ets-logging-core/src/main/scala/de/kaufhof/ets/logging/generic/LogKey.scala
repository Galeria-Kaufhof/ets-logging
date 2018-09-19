package de.kaufhof.ets.logging.generic

import de.kaufhof.ets.logging.util.Lazy

case class LogKey[I, E](id: String, encoder: LogEncoder[I, E]) {
  def ->(value: I): EagerPrimitive[I, E] = EagerPrimitive(this, value)
  def ~>(value: => I): LazyPrimitive[I, E] = LazyPrimitive(this, Lazy(value))
}
