package de.kaufhof.ets.logging.ext.actor

import akka.actor._
import de.kaufhof.ets.logging.syntax.{Decomposer2DecomposedImplicits, LogKeySyntax}

trait ActorDecomposers[E] extends Decomposer2DecomposedImplicits[E] with LogKeySyntax[E] {
  implicit val actorDecomposer: Decomposer[Actor]
  implicit def anyActor2Decomposer[A <: Actor]: Decomposer[A] = Decomposer[Actor].forSubType
}
