package de.kaufhof.ets.logging.test

import java.util.UUID

object domain {
  sealed trait Epic extends Product
  object Epic {
    case object FeatureA extends Epic
    case object FeatureB extends Epic
  }
  case class VariantId(value: String)
  case class Variant(id: VariantId, name: String)
  val variant: Variant = Variant(VariantId("VariantId"), "VariantName")
  val uuid: UUID = java.util.UUID.fromString("723f03f5-13a6-4e46-bdac-3c66718629df")
}
