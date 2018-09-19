package de.kaufhof.ets.logging.split

import de.kaufhof.ets.logging.split.generic._
import de.kaufhof.ets.logging.split.syntax._

trait PairLogEventCombiner[E, C] extends LogEventCombiner[E, C] with LogCombinedTypeDefinition[C] {
  type Pair

  protected def primToPair[I](p: Primitive[I]): Pair
  protected def combinePairs(encoded: Seq[Pair]): Combined
  private def toPrims(m: Event): Prims = m.map(_._2)(scala.collection.breakOut)
  override def combine(e: LogEvent[Encoded]): Combined = {
    val prims: Prims = toPrims(e)
    val encoded: Seq[Pair] = prims.map(primToPair(_))
    combinePairs(encoded)
  }
}
