package de.kaufhof.ets.logging.split

import java.time.LocalDateTime

import de.kaufhof.ets.logging.split.generic._
import de.kaufhof.ets.logging.split.syntax._

trait DefaultAttributeGatherer[E]
    extends LogTypeDefinitions[E]
    with LogAttributeGatherer[E]
    with PredefKeysInstance[E] {
  override def gatherGlobal: Seq[Attribute] = Seq(
    predefKeys.Timestamp ~> LocalDateTime.now()
  )
}
