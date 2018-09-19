package de.kaufhof.ets.logging

object StdOutStringLogAppender extends LogAppender[String, Unit] {
  override def append(combined: String): Unit = println(combined)
  override def ignore: Unit = ()
}
