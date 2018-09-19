package de.kaufhof.ets.logging.split.generic

trait LogAttributeGatherer[E] {
  def gatherGlobal: Seq[LogAttribute[E]]
}
