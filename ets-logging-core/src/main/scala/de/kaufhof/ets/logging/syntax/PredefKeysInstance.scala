package de.kaufhof.ets.logging.syntax

import de.kaufhof.ets.logging.generic.LogPredefKeys

trait PredefKeysInstance[E] {
  def predefKeys: LogPredefKeys[E]
}
