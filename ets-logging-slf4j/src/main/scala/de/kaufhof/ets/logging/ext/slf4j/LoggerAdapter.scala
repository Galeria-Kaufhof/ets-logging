package de.kaufhof.ets.logging.ext.slf4j

import de.kaufhof.ets.logging
import de.kaufhof.ets.logging.Level
import de.kaufhof.ets.logging.generic.LogAttribute
import org.slf4j.Marker

class LoggerAdapter[E, O](name: String, etsSlf4jSpiProvider: EtsSlf4jSpiProvider[E, O]) extends org.slf4j.Logger {
  override def getName: String = name

  private val config = etsSlf4jSpiProvider.config
  private val internal = config.createLogger(name)

  private def log(level: Level, attrs: LogAttribute[E]*): Unit = {
    etsSlf4jSpiProvider.outputToUnit(
      internal.event((attrs :+ config.predefKeys.Level -> level):_*)
    )
  }

  private def log(level: Level, msg: String, attrs: LogAttribute[E]*): Unit = {
    log(level, (attrs :+ config.predefKeys.Message -> msg):_*)
  }

  private def log(level: Level, msg: String, throwable: Throwable, attrs: LogAttribute[E]*): Unit = {
    log(level, msg, config.predefKeys.Throwable ~> throwable)
  }

  private def logWithFormat(level: Level, format: String, args: Object*): Unit = {
    log(level, config.predefKeys.Message ~> format.format(args:_*))
  }

  private def log(level: Level, marker: Marker, msg: String, attrs: LogAttribute[E]*): Unit = {
    log(level, msg, attributesForMarker(marker):_*)
  }

  private def log(level: Level, marker: Marker, msg: String, throwable: Throwable): Unit = {
    log(level, msg, throwable, attributesForMarker(marker):_*)
  }

  private def logWithFormat(level: Level, marker: Marker, format: String, args: Object*): Unit = {
    log(level, (attributesForMarker(marker) :+ config.predefKeys.Message ~> format.format(args:_*)):_*)
  }

  private def isLevelEnabled(level: Level): Boolean =
    config.loggerLevel(name) forwards level

  private def isLevelEnabled(level: Level, marker: Marker): Boolean =
    etsSlf4jSpiProvider.levelForLogger(getName, marker) forwards level

  private def attributesForMarker(marker: Marker): Seq[LogAttribute[E]] =
    etsSlf4jSpiProvider.logMarker.lift(marker).toSeq


  override def isTraceEnabled: Boolean = isLevelEnabled(Level.Trace)

  override def trace(msg: String): Unit = log(Level.Trace, msg)

  override def trace(format: String, arg: Object): Unit = logWithFormat(Level.Trace, format, arg)

  override def trace(format: String, arg1: Object, arg2: Object): Unit = logWithFormat(Level.Trace, format, arg1, arg2)

  override def trace(format: String, arguments: Object*): Unit = logWithFormat(Level.Trace, format, arguments:_*)

  override def trace(msg: String, t: Throwable): Unit = log(Level.Trace, msg, t)

  override def isTraceEnabled(marker: Marker): Boolean = isLevelEnabled(Level.Trace, marker)

  override def trace(marker: Marker, msg: String): Unit = log(Level.Trace, marker, msg)

  override def trace(marker: Marker, format: String, arg: Object): Unit = logWithFormat(Level.Trace, marker, format, arg)

  override def trace(marker: Marker, format: String, arg1: Object, arg2: Object): Unit = logWithFormat(Level.Trace, marker, format, arg1, arg2)

  override def trace(marker: Marker, format: String, arguments: Object*): Unit = logWithFormat(Level.Trace, marker, format, arguments:_*)

  override def trace(marker: Marker, msg: String, t: Throwable): Unit = log(Level.Trace, marker, msg, t)


  override def isDebugEnabled: Boolean = isLevelEnabled(Level.Debug)

  override def debug(msg: String): Unit = log(Level.Debug, msg)

  override def debug(format: String, arg: Object): Unit = logWithFormat(Level.Debug, format, arg)

  override def debug(format: String, arg1: Object, arg2: Object): Unit = logWithFormat(Level.Debug, format, arg1, arg2)

  override def debug(format: String, arguments: Object*): Unit = logWithFormat(Level.Debug, format, arguments:_*)

  override def debug(msg: String, t: Throwable): Unit = log(Level.Debug, msg, t)

  override def isDebugEnabled(marker: Marker): Boolean = isLevelEnabled(Level.Debug, marker)

  override def debug(marker: Marker, msg: String): Unit = log(Level.Debug, marker, msg)

  override def debug(marker: Marker, format: String, arg: Object): Unit = logWithFormat(Level.Debug, marker, format, arg)

  override def debug(marker: Marker, format: String, arg1: Object, arg2: Object): Unit = logWithFormat(Level.Debug, marker, format, arg1, arg2)

  override def debug(marker: Marker, format: String, arguments: Object*): Unit = logWithFormat(Level.Debug, marker, format, arguments:_*)

  override def debug(marker: Marker, msg: String, t: Throwable): Unit = log(Level.Debug, marker, msg, t)


  override def isInfoEnabled: Boolean = isLevelEnabled(Level.Info)

  override def info(msg: String): Unit = log(Level.Info, msg)

  override def info(format: String, arg: Object): Unit = logWithFormat(Level.Info, format, arg)

  override def info(format: String, arg1: Object, arg2: Object): Unit = logWithFormat(Level.Info, format, arg1, arg2)

  override def info(format: String, arguments: Object*): Unit = logWithFormat(Level.Info, format, arguments:_*)

  override def info(msg: String, t: Throwable): Unit = log(Level.Info, msg, t)

  override def isInfoEnabled(marker: Marker): Boolean = isLevelEnabled(Level.Info, marker)

  override def info(marker: Marker, msg: String): Unit = log(Level.Info, marker, msg)

  override def info(marker: Marker, format: String, arg: Object): Unit = logWithFormat(Level.Info, marker, format, arg)

  override def info(marker: Marker, format: String, arg1: Object, arg2: Object): Unit = logWithFormat(Level.Info, marker, format, arg1, arg2)

  override def info(marker: Marker, format: String, arguments: Object*): Unit = logWithFormat(Level.Info, marker, format, arguments:_*)

  override def info(marker: Marker, msg: String, t: Throwable): Unit = log(Level.Info, marker, msg, t)

  
  override def isWarnEnabled: Boolean = isLevelEnabled(Level.Warn)

  override def warn(msg: String): Unit = log(Level.Warn, msg)

  override def warn(format: String, arg: Object): Unit = logWithFormat(Level.Warn, format, arg)

  override def warn(format: String, arg1: Object, arg2: Object): Unit = logWithFormat(Level.Warn, format, arg1, arg2)

  override def warn(format: String, arguments: Object*): Unit = logWithFormat(Level.Warn, format, arguments:_*)

  override def warn(msg: String, t: Throwable): Unit = log(Level.Warn, msg, t)

  override def isWarnEnabled(marker: Marker): Boolean = isLevelEnabled(Level.Warn, marker)

  override def warn(marker: Marker, msg: String): Unit = log(Level.Warn, marker, msg)

  override def warn(marker: Marker, format: String, arg: Object): Unit = logWithFormat(Level.Warn, marker, format, arg)

  override def warn(marker: Marker, format: String, arg1: Object, arg2: Object): Unit = logWithFormat(Level.Warn, marker, format, arg1, arg2)

  override def warn(marker: Marker, format: String, arguments: Object*): Unit = logWithFormat(Level.Warn, marker, format, arguments:_*)

  override def warn(marker: Marker, msg: String, t: Throwable): Unit = log(Level.Warn, marker, msg, t)


  override def isErrorEnabled: Boolean = isLevelEnabled(Level.Error)

  override def error(msg: String): Unit = log(Level.Error, msg)

  override def error(format: String, arg: Object): Unit = logWithFormat(Level.Error, format, arg)

  override def error(format: String, arg1: Object, arg2: Object): Unit = logWithFormat(Level.Error, format, arg1, arg2)

  override def error(format: String, arguments: Object*): Unit = logWithFormat(Level.Error, format, arguments:_*)

  override def error(msg: String, t: Throwable): Unit = log(Level.Error, msg, t)

  override def isErrorEnabled(marker: Marker): Boolean = isLevelEnabled(Level.Error, marker)

  override def error(marker: Marker, msg: String): Unit = log(Level.Error, marker, msg)

  override def error(marker: Marker, format: String, arg: Object): Unit = logWithFormat(Level.Error, marker, format, arg)

  override def error(marker: Marker, format: String, arg1: Object, arg2: Object): Unit = logWithFormat(Level.Error, marker, format, arg1, arg2)

  override def error(marker: Marker, format: String, arguments: Object*): Unit = logWithFormat(Level.Error, marker, format, arguments:_*)

  override def error(marker: Marker, msg: String, t: Throwable): Unit = log(Level.Error, marker, msg, t)
}
