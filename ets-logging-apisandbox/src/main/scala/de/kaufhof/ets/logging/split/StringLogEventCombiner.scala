package de.kaufhof.ets.logging.split

object StringLogEventCombiner extends PairLogEventCombiner[String, String] {
  override type Pair = String
  override protected def primToPair[I](p: Primitive[I]): Pair = s"${p.key.id} -> ${p.encoded}"
  override protected def combinePairs(encoded: Seq[Pair]): Combined = encoded.sorted.mkString(" | ")
}
