package de.kaufhof.ets.logging.split.ext.actor

import akka.actor._
import de.kaufhof.ets.logging.split.generic.LogKey

trait ActorPredefKeys[E] {
  val ActorSource: LogKey[ActorPath, E]
}
