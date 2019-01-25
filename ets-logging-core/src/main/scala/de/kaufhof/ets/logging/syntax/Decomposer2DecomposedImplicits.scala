package de.kaufhof.ets.logging.syntax

import de.kaufhof.ets.logging.generic

import scala.language.implicitConversions

trait Decomposer2DecomposedImplicits[E] extends LogTypeDefinitions[E] {
  implicit def anyDecomposer2decomposed[I: Decomposer](obj: I): Decomposed[I] = implicitly[Decomposer[I]].encode(obj)
  object Decomposed {
    def apply[I](primitives: Primitive[_]*): Decomposed[I] = generic.LogDecomposed(primitives: _*)
  }
  object Decomposer {
    class DecomposerHalfApplied[A] {
      def forSubType[Sub <: A](implicit d: Decomposer[A]): Decomposer[Sub] = d.comap(identity)
    }
    def apply[A]: DecomposerHalfApplied[A] = new DecomposerHalfApplied
  }
  implicit class RichDecomposer[A](d: Decomposer[A]) {
    def comap[B](f: B => A): Decomposer[B] = (b: B) => Decomposed(d.encode(f(b)).primitives: _*)
  }
}
