package de.kaufhof.ets.logging.syntax

trait LogEventOps[E] extends LogTypeDefinitions[E] {
  private def primsToEvent(prims: Prims): Event = prims.map(primToTuple(_)).toMap
  def primToTuple[I](p: Primitive[I]): (Key[I], Primitive[I]) = (p.key, p)
  def fromAttributes(attributes: Seq[Attribute]): Event = primsToEvent(attributesToPrims(attributes))
  def aggregate(acc: Event, next: Event): Event = acc ++ next
  private def attributesToPrims(attributes: Seq[Attribute]): Prims =
    attributes.foldLeft(Seq.empty: Prims) { (acc, attr) =>
      attr match {
        case p: Primitive[_] => acc :+ p
        case c: Composite => acc ++ c.primitives
      }
    }
}
