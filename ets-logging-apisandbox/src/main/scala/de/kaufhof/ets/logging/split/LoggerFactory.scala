package de.kaufhof.ets.logging.split

import de.kaufhof.ets.logging.split.syntax.LogTypeDefinitionsExt

trait LoggerFactory[E, O] extends LogTypeDefinitionsExt[E, O] {
  def createLogger(cls: Class[_]): Logger
  trait LogInstance {
    protected lazy val log: Logger = createLogger(getClass)
  }
}
