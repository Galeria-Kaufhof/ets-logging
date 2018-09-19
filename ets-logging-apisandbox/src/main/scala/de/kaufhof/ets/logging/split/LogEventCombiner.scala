package de.kaufhof.ets.logging.split

import de.kaufhof.ets.logging.split.syntax._

trait LogEventCombiner[E, C] extends LogTypeDefinitions[E] with LogCombinedTypeDefinition[C] {
  def combine(e: Event): Combined
}
