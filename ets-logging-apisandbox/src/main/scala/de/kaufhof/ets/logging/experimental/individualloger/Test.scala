package de.kaufhof.ets.logging.experimental.individualloger

object Api {
  trait Logger {
    def error(msg: String, kvs: (String, String)*): Unit
  }
  trait LoggerFactory {
    def create[T](obj: T, staticKvs: (String, String)*): Logger
  }
}

object LogstashApi {
  import Api._
  import net.logstash.logback.marker.MapEntriesAppendingMarker
  import scala.collection.JavaConverters._

  class LogstashLogger(log: org.slf4j.Logger, staticKvs: (String, String)*) extends Logger {
    override def error(msg: String, kvs: (String, String)*): Unit = {
      val map: Map[String, String] = (staticKvs ++ kvs).toMap
      val marker = new MapEntriesAppendingMarker(map.asJava)
      log.error(marker, msg)
    }
  }

  object LogstashLoggerFactory extends LoggerFactory {
    override def create[T](obj: T, staticKvs: (String, String)*): Logger =
      new LogstashLogger(org.slf4j.LoggerFactory.getLogger(obj.getClass), staticKvs :_*)
  }

  trait LogInstance {
    lazy val log: Logger = LogstashLoggerFactory.create(this)
  }
}

object ClientApi {}

object MainUsage {
  import LogstashApi._

  object A extends LogInstance {}
}
