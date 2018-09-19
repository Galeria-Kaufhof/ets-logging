package de.kaufhof.ets.logging.syntax

import scala.language.implicitConversions

trait Decomposer2DecomposedImplicits[E] extends LogTypeDefinitions[E] {
  implicit def anyDecomposer2decomposed[I: Decomposer](obj: I): Decomposed[I] = implicitly[Decomposer[I]].encode(obj)
}
