package de.kaufhof.ets.logging.split

sealed abstract class Level(val priority: Int) extends Ordering[Level] {
  override def compare(x: Level, y: Level): Int = x.priority compare y.priority
  def forwards(other: Level): Boolean = this <= other
}
object Level {
  case object Trace extends Level(0)
  case object Debug extends Level(1)
  case object Info extends Level(2)
  case object Warn extends Level(3)
  case object Error extends Level(4)

  val Lowest: Trace.type = Trace
  val Highest: Error.type = Error
}
