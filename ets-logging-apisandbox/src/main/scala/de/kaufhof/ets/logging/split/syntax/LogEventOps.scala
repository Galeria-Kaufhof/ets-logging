package de.kaufhof.ets.logging.split.syntax

trait LogEventOps[E] extends LogTypeDefinitions[E] {
  private def primsToEvent(prims: Prims): Event = prims.map(p => (p.key, p)).toMap
  def fromAttributes(attributes: Seq[Attribute]): Event = primsToEvent(attributesToPrims(attributes))
  def aggregate(acc: Event, next: Event): Event = acc ++ next
  private def attributesToPrims(attributes: Seq[Attribute]): Prims = {
    val zero: Prims = Seq.empty
    attributes.foldLeft(zero) { (acc, attr) =>
      attr match {
        case p: Primitive[_] => acc :+ p
        case c: Composite[_] => acc ++ c.primitives
      }
    }
  }
}
