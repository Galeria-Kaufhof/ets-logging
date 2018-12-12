package de.kaufhof.ets.logging

import java.time.Instant

import de.kaufhof.ets.logging.generic._
import de.kaufhof.ets.logging.syntax._

trait DefaultAttributeGatherer[E]
    extends LogTypeDefinitions[E]
    with LogAttributeGatherer[E]
    with PredefKeysInstance[E] {
  override def gatherGlobal: Seq[Attribute] = Seq(
    predefKeys.Timestamp ~> Instant.now()
  )
}
