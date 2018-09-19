package de.kaufhof.ets.logging.split.syntax

import de.kaufhof.ets.logging.split.generic

trait LogTypeDefinitionsExt[E, O] extends LogTypeDefinitions[E] {
  type Output = O
  type Logger = generic.Logger[Encoded, Output]
}
