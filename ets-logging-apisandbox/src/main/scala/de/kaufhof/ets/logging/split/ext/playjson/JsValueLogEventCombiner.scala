package de.kaufhof.ets.logging.split.ext.playjson

import de.kaufhof.ets.logging.split._
import play.api.libs.json._

object JsValueLogEventCombiner extends PairLogEventCombiner[JsValue, JsValue] {
  override type Pair = (String, JsValue)
  override protected def primToPair[I](p: Primitive[I]): Pair = p.key.id -> p.encoded
  override protected def combinePairs(encoded: Seq[Pair]): Combined = JsObject(encoded)
}
