package de.kaufhof.ets.logging.ext.circejson

import de.kaufhof.ets.logging.LogAppender
import io.circe.Json

object StdOutJsonLogAppender extends LogAppender[Json, Unit] {
  override def append(combined: Json): Unit = println(combined.noSpaces)
  override def ignore: Unit = ()
}
