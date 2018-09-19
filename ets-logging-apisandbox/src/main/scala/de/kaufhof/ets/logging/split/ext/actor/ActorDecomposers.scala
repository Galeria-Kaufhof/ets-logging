package de.kaufhof.ets.logging.split.ext.actor

import akka.actor._
import de.kaufhof.ets.logging.split.syntax.{Decomposer2DecomposedImplicits, LogKeySyntax}

trait ActorDecomposers[E] extends Decomposer2DecomposedImplicits[E] with LogKeySyntax[E] {
  implicit val actorDecomposer: Decomposer[Actor]
  implicit def anyActor2Decomposer[A <: Actor]: Decomposer[A] =
    actor => Decomposed(actorDecomposer.encode(actor).primitives: _*)
}
