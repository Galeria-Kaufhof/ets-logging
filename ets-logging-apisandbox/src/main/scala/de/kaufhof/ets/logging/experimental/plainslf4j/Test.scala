package de.kaufhof.ets.logging.experimental.plainslf4j

import de.kaufhof.ets.logging.experimental.Benchmark._

import net.logstash.logback.marker.MapEntriesAppendingMarker
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object Api {
  class Logger(log: org.slf4j.Logger) {
    def error(msg: String, kvs: (String, String)*): Unit = {
      val map: Map[String, String] = kvs.toMap
      val marker = new MapEntriesAppendingMarker(map.asJava)
      log.error(marker, msg)
    }
  }

  trait LogInstance {
    def log: Logger = new Logger(LoggerFactory.getLogger(getClass))
  }
}

object Main extends App with Api.LogInstance {
  case class VariantId(value: String)
  case class Variant(id: VariantId, name: String)
  val uuid = java.util.UUID.fromString("723f03f5-13a6-4e46-bdac-3c66718629df")
  val variant = Variant(VariantId("VariantId"), "VariantName")


  this.benchmark {
    log.error("someError", "variantid" -> variant.id.value, "variantname" -> variant.name)
    log.error("someError", "variantid" -> variant.id.value, "variantname" -> variant.name, "uuid" -> "abcdefg")
  }
}
