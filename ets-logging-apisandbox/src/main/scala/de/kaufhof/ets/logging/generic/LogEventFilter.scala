package de.kaufhof.ets.logging.generic

trait LogEventFilter[E] {
  def forwards(event: LogEvent[E]): Boolean
}
