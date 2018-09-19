package de.kaufhof.ets.logging.split.syntax

import de.kaufhof.ets.logging.split.generic.LogPredefKeys

trait PredefKeysInstance[E] {
  def predefKeys: LogPredefKeys[E]
}
