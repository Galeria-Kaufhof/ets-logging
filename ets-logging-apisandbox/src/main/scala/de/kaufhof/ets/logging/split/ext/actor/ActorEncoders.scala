package de.kaufhof.ets.logging.split.ext.actor

import akka.actor._
import de.kaufhof.ets.logging.split.syntax.DefaultEncoders

trait ActorEncoders[E] extends DefaultEncoders[E] {
  def createActorPathEncoder: Encoder[ActorPath] = Encoder.fromToString
  implicit lazy val actorPathEncoder: Encoder[ActorPath] = createActorPathEncoder
}
