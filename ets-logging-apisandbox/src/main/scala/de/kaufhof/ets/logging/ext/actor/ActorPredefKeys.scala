package de.kaufhof.ets.logging.ext.actor

import akka.actor._
import de.kaufhof.ets.logging.generic.LogKey

trait ActorPredefKeys[E] {
  val ActorSource: LogKey[ActorPath, E]
}
