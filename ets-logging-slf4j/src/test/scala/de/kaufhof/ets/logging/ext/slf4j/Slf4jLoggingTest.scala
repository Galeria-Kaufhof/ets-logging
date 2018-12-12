package de.kaufhof.ets.logging.ext.slf4j

import java.io.IOException

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

class Slf4jLoggingTest extends FlatSpec with BeforeAndAfter with Matchers with StringLogConfig.LogInstance {

  behavior of "ETS SLF4J Logging"

  before {
    RecordingAppender.clear()
  }

  it should "log statements" in {
    val etsLoggerName = getClass.getName
    val slf4jLoggerName = "slf4j-test"

    val etsLoggerStatements = List(
      "ets log test 1",
      "ets log test 2"
    )

    val slf4jMarkerEvents = List(
      (new TestMarker("test-marker"), "slf4j marker log test 1"),
      (new ExtendedTestMarker, "slf4j marker log test 2")
    )

    val slf4jFormatEvents = List(
      ("%1$s", "test 1"),
      ("%1$h", "2748")
    )

    val slf4jThrowableEvents = List(
      ("runtime", new RuntimeException),
      ("io", new IOException)
    )

    etsLoggerStatements.foreach { event =>
      log.info(event)
    }

    slf4jMarkerEvents.foreach { case (marker, event) =>
      LoggerFactory.getLogger(slf4jLoggerName).info(marker, event)
    }

    slf4jFormatEvents.foreach { case (format, event) =>
      LoggerFactory.getLogger(slf4jLoggerName).info(format, event)
    }

    slf4jThrowableEvents.foreach { case (event, throwable) =>
      LoggerFactory.getLogger(slf4jLoggerName).info(event, throwable)
    }

    val expectedResult =
      s"""
        |level -> Info | logger -> $etsLoggerName | msg -> ets log test 1 | ts -> 2018-12-06T08:14:21.770Z
        |level -> Info | logger -> $etsLoggerName | msg -> ets log test 2 | ts -> 2018-12-06T08:14:21.770Z
        |level -> Info | logger -> $slf4jLoggerName | msg -> slf4j marker log test 1 | testmarker -> test-marker | ts -> 2018-12-06T08:14:21.770Z
        |extended-testmarker -> extended-test-marker | level -> Info | logger -> $slf4jLoggerName | msg -> slf4j marker log test 2 | ts -> 2018-12-06T08:14:21.770Z
        |level -> Info | logger -> $slf4jLoggerName | msg -> test 1 | ts -> 2018-12-06T08:14:21.770Z
        |level -> Info | logger -> $slf4jLoggerName | msg -> 178f89 | ts -> 2018-12-06T08:14:21.770Z
        |level -> Info | logger -> $slf4jLoggerName | msg -> runtime | throwable -> java.lang.RuntimeException | ts -> 2018-12-06T08:14:21.770Z
        |level -> Info | logger -> $slf4jLoggerName | msg -> io | throwable -> java.io.IOException | ts -> 2018-12-06T08:14:21.770Z
      """.stripMargin.stripPrefix("\n").stripSuffix("\n      ")

    RecordingAppender.records.mkString("\n") shouldBe expectedResult
  }

}
