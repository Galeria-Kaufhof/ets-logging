package de.kaufhof.ets.logging.split.test

import java.util.UUID

object domain {
  case class VariantId(value: String)
  case class Variant(id: VariantId, name: String)
  val variant: Variant = Variant(VariantId("VariantId"), "VariantName")
  val uuid: UUID = java.util.UUID.fromString("723f03f5-13a6-4e46-bdac-3c66718629df")
}
