package de.kaufhof.ets.logging.ext.actor

import akka.actor._
import de.kaufhof.ets.logging.syntax.DefaultEncoders

trait ActorEncoders[E] extends DefaultEncoders[E] {
  def createActorPathEncoder: Encoder[ActorPath] = Encoder.fromToString
  implicit lazy val actorPathEncoder: Encoder[ActorPath] = createActorPathEncoder
}
