package de.kaufhof.ets.logging.generic

import java.time.LocalDateTime

import de.kaufhof.ets.logging.Level

trait LogPredefKeys[E] {
  val Logger: LogKey[Class[_], E]
  val Level: LogKey[Level, E]
  val Message: LogKey[String, E]
  val Timestamp: LogKey[LocalDateTime, E]
}
