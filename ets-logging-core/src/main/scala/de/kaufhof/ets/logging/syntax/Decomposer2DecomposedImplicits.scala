package de.kaufhof.ets.logging.syntax

import de.kaufhof.ets.logging.generic

import scala.language.implicitConversions

trait Decomposer2DecomposedImplicits[E] extends LogTypeDefinitions[E] {
  implicit def anyDecomposer2decomposed[I: Decomposer](obj: I): Decomposed = implicitly[Decomposer[I]].encode(obj)
  object Decomposed {
    def apply(primitives: Primitive[_]*): Decomposed = generic.LogDecomposed(primitives: _*)
  }
}
