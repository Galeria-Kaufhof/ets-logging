package de.kaufhof.ets.logging.experimental

import java.io.File

import org.scalameter._

object Benchmark {
  implicit class AppOps[T <: App](app: T) {
    def benchmark(test: => Unit): Unit = {
      val logFile = new File("testLog.log")
      if(logFile.exists()) logFile.delete()

      val time = config(
        Key.exec.benchRuns -> 200000,
        Key.exec.minWarmupRuns -> 1000,
        Key.verbose -> true
      ) withWarmer {
        new Warmer.Default
      } withMeasurer {
        new Measurer.IgnoringGC
      } measure {
        test
      }

      println(app.getClass)
      println(s"Total time: $time")
    }
  }
}
