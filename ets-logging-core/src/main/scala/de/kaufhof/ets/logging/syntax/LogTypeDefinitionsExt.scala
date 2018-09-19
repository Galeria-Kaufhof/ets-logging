package de.kaufhof.ets.logging.syntax

import de.kaufhof.ets.logging.generic

trait LogTypeDefinitionsExt[E, O] extends LogTypeDefinitions[E] {
  type Output = O
  type Logger = generic.Logger[Encoded, Output]
}
