package de.kaufhof.ets.logging.syntax

import de.kaufhof.ets.logging.util.Lazy

class ConfigSyntax[K, D](createKeys: Lazy[K], createDecomposers: Lazy[D]) {
  val Keys: K = createKeys()
  val decomposers: D = createDecomposers()
}
object ConfigSyntax {
  def apply[K, D](keys: => K, decomposers: => D) = new ConfigSyntax[K, D](keys _, decomposers _)
}
