package de.kaufhof.ets.logging.generic

import java.time.Instant

import de.kaufhof.ets.logging.Level

trait LogPredefKeys[E] {
  val Logger: LogKey[String, E]
  val Level: LogKey[Level, E]
  val Message: LogKey[String, E]
  val Throwable: LogKey[Throwable, E]
  val Timestamp: LogKey[Instant, E]
}
