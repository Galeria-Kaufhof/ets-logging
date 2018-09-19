package de.kaufhof.ets.logging.split

import de.kaufhof.ets.logging.split.syntax.LogCombinedTypeDefinition

// TODO: evaluate if self types or inheritance is more appropriate except for LogAttribute which is modelled as sealed trait on purpose
trait LogAppender[C, O] extends LogCombinedTypeDefinition[C] {
  self =>
  def append(combined: Combined): O
  def ignore: O
  def comap[I](f: I => Combined): LogAppender[I, O] = new LogAppender[I, O] {
    override def append(combined: I): O = self.append(f(combined))
    override def ignore: O = self.ignore
  }
}
