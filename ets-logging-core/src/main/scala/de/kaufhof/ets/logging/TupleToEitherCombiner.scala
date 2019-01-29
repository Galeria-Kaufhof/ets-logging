package de.kaufhof.ets.logging

import de.kaufhof.ets.logging.generic.{LogEvent, LogKey, LogPrimitive}

trait TupleToEitherCombiner[E1, E2] extends LogEventCombiner[(E1, E2), Either[E1, E2]] {
  def combiner1: LogEventCombiner[E1, E1]
  def combiner2: LogEventCombiner[E2, E2]
  def takeLeft(e: LogEvent[(E1, E2)]): Boolean

  def select(e: LogEvent[(E1, E2)]): Combined = {
    def transformEventBy[B](selector: ((E1, E2)) => B): LogEvent[B] =
      e.map {
        case (_, value: LogPrimitive[_, (E1, E2)]) => primToTuple(value.mapEncoded(selector))
      }.toMap
    def part[A](c: LogEventCombiner[A, A])(selector: ((E1, E2)) => A) =
      c.combine(transformEventBy(selector))
    if (takeLeft(e)) Left(part(combiner1)(_._1))
    else Right(part(combiner2)(_._2))
  }
  def primToTuple[I, E](p: LogPrimitive[I, E]): (LogKey[I, E], LogPrimitive[I, E]) = (p.key, p)
  override def combine(e: LogEvent[(E1, E2)]): Combined = select(e)
}
