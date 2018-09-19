package de.kaufhof.ets.logging.syntax

import de.kaufhof.ets.logging.generic

trait LogKeySyntax[E] extends LogTypeDefinitions[E] {
  object Key {
    def apply(id: String): KeyOps = new KeyOps(id)
  }
  class KeyOps(id: String) {
    def withExplicit[I](encoder: Encoder[I]): Key[I] = generic.LogKey(id, encoder)
    def withImplicitEncoder[I](implicit encoder: Encoder[I]): Key[I] = generic.LogKey(id, encoder)
  }
  object Decomposed {
    def apply[I](primitives: Primitive[_]*): Decomposed[I] = generic.LogDecomposed(primitives: _*)
  }
}
