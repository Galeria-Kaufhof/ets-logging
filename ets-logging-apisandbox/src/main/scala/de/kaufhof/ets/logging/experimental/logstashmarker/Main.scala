package de.kaufhof.ets.logging.experimental.logstashmarker

import java.time.LocalTime
import java.util.UUID

import net.logstash.logback.marker.MapEntriesAppendingMarker
import net.logstash.logback.marker.Markers._
import org.slf4j.Marker

import scala.collection.JavaConverters._

object Main extends App {
  val time = LocalTime.parse("11:14:32.591")
  val markerFromString = {
    val stringInput: Map[String, String] = Map(
      "variantid" -> "VariantId",
      "variantname" -> "VariantName",
      "variantprice" -> "1234",
      "uuid" -> "723f03f5-13a6-4e46-bdac-3c66718629df",
      "decimal" -> "1.12345678901234567890",
      "nested.key" -> "content",
      "sometime" -> time.toString
    )
    new MapEntriesAppendingMarker(stringInput.asJava)
  }
  val markerFromAny = {
    val anyInput: Map[String, Any] = Map(
      "variantid" -> "VariantId",
      "variantname" -> "VariantName",
      "variantprice" -> 1234L,
      "uuid" -> UUID.fromString("723f03f5-13a6-4e46-bdac-3c66718629df"),
      "decimal" -> 1.12345678901234567890,
      "nested" -> Map("key" -> "content"),
      "sometime" -> time
    )
    new MapEntriesAppendingMarker(anyInput.asJava)
  }
  val markerFromMarkersFluentInterface = {
    val anyInput: Map[String, Any] = Map(
      "variantid" -> "VariantId",
      "variantname" -> "VariantName",
      "variantprice" -> 1234L,
      "uuid" -> UUID.fromString("723f03f5-13a6-4e46-bdac-3c66718629df"),
      "decimal" -> 1.12345678901234567890,
      "nested" -> Map("key" -> "content"),
      "sometime" -> time
    )
    appendEntries(anyInput.asJava)
  }

  val emptyMarker = {
    new MapEntriesAppendingMarker(Map.empty.asJava)
  }

  def printMarker(marker: Marker): Unit = {
    def printRecursive(marker: Marker, level: Int = 0): Unit = {
      val indention = "  " * level
      println(s"$indention${marker.getName}${marker.hasReferences}")
      marker.iterator().asScala.foreach(printRecursive(_, level + 1))
    }
    println(marker)
    printRecursive(marker)
  }

  printMarker(markerFromString)
  printMarker(markerFromAny)
  printMarker(emptyMarker)
  printMarker(markerFromMarkersFluentInterface)

  val sample = """{
    |    "bid": "0100007FECC4405B191A5904026C5B03",
    |    "debid": "7700002923",
    |    "epic": "BSNA",
    |    "level": "INFO",
    |    "lineItems": [
    |        {
    |            "articleId": "1002617403",
    |            "quantity": 2,
    |            "value": 2398,
    |            "variantId": "1002619660"
    |        }
    |    ],
    |    "logger": "d.g.d.o.CreateOrderService",
    |    "msg": "Order created",
    |    "numberOfItems": 1,
    |    "oid": "3400984132",
    |    "or.channel": "Default",
    |    "orderDiscount": 0.0,
    |    "orderValue": 23.98,
    |    "retention": "KPI",
    |    "tid": "1530972725.194-6683-212839578-10",
    |    "tier": "domain",
    |    "ts": "2018-07-07T14:12:04.048+00:00",
    |    "type": "OrderPlaced",
    |    "uid": "2a0e98c8-4790-4819-b6f5-5b03cf6fd8a2",
    |    "universe": "ShopEpics",
    |    "vid": "fwAAAVtAxOwEWRoZA1tsAg=="
    |}"""

}
