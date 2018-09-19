package de.kaufhof.ets.logging

import de.kaufhof.ets.logging.generic._
import de.kaufhof.ets.logging.syntax._

trait AbstractLogAttributeProcessor[E, O]
    extends LogAttributeProcessor[E, O]
    with LogTypeDefinitionsExt[E, O]
    with LogEventFilter[E]
    with LogAttributeGatherer[E] {
  type Combined
  final type EventCombiner = LogEventCombiner[Encoded, Combined]
  final type Appender = LogAppender[Combined, Output]

  def combiner: EventCombiner
  def appender: Appender

  private object Event extends LogEventOps[Encoded]

  // TODO: describe this process in the readme:
  // TODO:   - types: Attribute, Primitive, Event, Decomposer
  // TODO:   - attribute scopes: local, global (still missing: logger, logger implicits, local implicits)
  // TODO:     maybe rename local to statement because statement is the "real" scope
  // TODO:   - processing pipeline
  // TODO:     log events can be generated from any amount of attributes
  // TODO:     attributes -> primitives -> primitives with keys => map = event
  // TODO:   - merge semantics
  // TODO:     local attributes overwrite more global attributes
  // TODO:     (still missing: explicit attributes overwrite implicit attributes)
  // TODO:
  def process(attributes: Seq[Attribute]): O = {
    val local = Event.fromAttributes(attributes)
    if (forwards(local)) {
      val global = Event.fromAttributes(gatherGlobal)
      val merged = Event.aggregate(global, local)
      appender.append(combiner.combine(merged))
    } else appender.ignore
  }
}
