package de.kaufhof.ets.logging.ext.circejson

import de.kaufhof.ets.logging._
import io.circe.Json

object JsonLogEventCombiner extends PairLogEventCombiner[Json, Json] {
  override type Pair = (String, Json)
  override protected def primToPair[I](p: Primitive[I]): Pair = p.key.id -> p.encoded
  override protected def combinePairs(encoded: Seq[Pair]): Combined = Json.fromFields(encoded)
}
