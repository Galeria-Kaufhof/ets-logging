package de.kaufhof.ets.logging.split.ext.catsio

import cats.effect.IO
import de.kaufhof.ets.logging.split.LogAppender

object CatsIoAppender extends LogAppender[String, IO[Unit]] {
  override def append(combined: String): IO[Unit] = IO(println(combined))
  override def ignore: IO[Unit] = IO(())
}
