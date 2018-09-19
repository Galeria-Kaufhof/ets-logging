package de.kaufhof.ets.logging

import de.kaufhof.ets.logging.syntax._

trait LogEventCombiner[E, C] extends LogTypeDefinitions[E] with LogCombinedTypeDefinition[C] {
  def combine(e: Event): Combined
}
