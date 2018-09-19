package de.kaufhof.ets.logging.generic

trait LogAttributeGatherer[E] {
  def gatherGlobal: Seq[LogAttribute[E]]
}
