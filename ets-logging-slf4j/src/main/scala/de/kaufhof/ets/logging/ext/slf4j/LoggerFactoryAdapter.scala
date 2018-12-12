package de.kaufhof.ets.logging.ext.slf4j

import java.util.concurrent.ConcurrentHashMap

import org.slf4j

class LoggerFactoryAdapter[E, O](etsSlf4jSpiProvider: EtsSlf4jSpiProvider[E, O]) extends slf4j.ILoggerFactory {

  private val loggerMap = new ConcurrentHashMap[String, slf4j.Logger]()

  override def getLogger(name: String): slf4j.Logger = {
    val logger = loggerMap.get(name)

    if (logger == null) {
      val newLogger = new LoggerAdapter(name, etsSlf4jSpiProvider)
      val previousLogger = loggerMap.putIfAbsent(name, newLogger)

      if (previousLogger == null) newLogger
      else previousLogger
    } else {
      logger
    }


  }
}
