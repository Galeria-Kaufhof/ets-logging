package de.kaufhof.ets.logging.split.syntax

import de.kaufhof.ets.logging.split.generic

trait LogTypeDefinitions[E] {
  final type Encoded = E
  final type Encoder[I] = generic.LogEncoder[I, Encoded]
  final type Primitive[I] = generic.LogPrimitive[I, Encoded]
  final type Key[I] = generic.LogKey[I, Encoded]
  final type Decomposed[I] = generic.LogDecomposed[I, Encoded]
  final type PredefKeys = generic.LogPredefKeys[Encoded]
  final type Attribute = generic.LogAttribute[Encoded]
  final type Decomposer[I] = generic.LogEncoder[I, Decomposed[I]]
  final type Event = generic.LogEvent[Encoded]
  final type EventFilter = generic.LogEventFilter[Encoded]
  final type AttributeGatherer = generic.LogAttributeGatherer[Encoded]
  final type Composite[I] = generic.LogDecomposed[I, Encoded]

  protected final type Prims = Seq[generic.LogPrimitive[_, Encoded]]
}
