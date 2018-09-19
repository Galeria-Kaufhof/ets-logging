package de.kaufhof.ets.logging.split.generic

trait LogEventFilter[E] {
  def forwards(event: LogEvent[E]): Boolean
}
