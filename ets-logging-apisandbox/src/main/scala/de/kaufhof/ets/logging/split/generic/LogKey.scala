package de.kaufhof.ets.logging.split.generic

import de.kaufhof.ets.logging.split.util.Lazy

case class LogKey[I, E](id: String, encoder: LogEncoder[I, E]) {
  def ->(value: I): EagerPrimitive[I, E] = EagerPrimitive(this, value)
  def ~>(value: => I): LazyPrimitive[I, E] = LazyPrimitive(this, Lazy(value))
}
