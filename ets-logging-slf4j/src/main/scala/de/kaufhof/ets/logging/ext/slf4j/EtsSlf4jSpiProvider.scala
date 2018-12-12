package de.kaufhof.ets.logging.ext.slf4j

import de.kaufhof.ets.logging.generic.LogAttribute
import de.kaufhof.ets.logging.{DefaultLogConfig, Level}
import org.slf4j
import org.slf4j.Marker
import org.slf4j.helpers.{BasicMDCAdapter, BasicMarkerFactory}
import org.slf4j.spi.{MDCAdapter, SLF4JServiceProvider}

object EtsSlf4jSpiProvider {
  type LogAttributeResolver[E] = PartialFunction[Marker, LogAttribute[E]]
}
abstract class EtsSlf4jSpiProvider[E, O] extends SLF4JServiceProvider {
  private var loggerFactory: slf4j.ILoggerFactory = _
  private var markerFactory: slf4j.IMarkerFactory = _
  private var mdcAdapter: slf4j.spi.MDCAdapter = _

  override def getLoggerFactory: slf4j.ILoggerFactory = loggerFactory
  override def getMarkerFactory: slf4j.IMarkerFactory = markerFactory
  override def getMDCAdapter: MDCAdapter = mdcAdapter
  override def getRequesteApiVersion: String = "1.8.0"

  override def initialize(): Unit = {
    loggerFactory = new LoggerFactoryAdapter(this)
    markerFactory = new BasicMarkerFactory
    mdcAdapter = new BasicMDCAdapter
  }

  def config: DefaultLogConfig[E, O]

  def outputToUnit(output: O): Unit = ()

  // Marker
  def logMarker: EtsSlf4jSpiProvider.LogAttributeResolver[E] =
    PartialFunction.empty

  def levelForLogger(name: String, marker: Marker): Level =
    config.loggerLevel(name)

}
