package de.kaufhof.ets.logging.split

import de.kaufhof.ets.logging.split.generic._
import de.kaufhof.ets.logging.split.syntax._
import de.kaufhof.ets.logging.split.util._

trait ClassLevelLogEventFilter[E]
    extends LogEventFilter[E]
    with ClassNameExtractor
    with PredefKeysInstance[E]
    with LogTypeDefinitions[E] {
  def rootLevel: Level = Level.Info
  def classNameLevels: Map[String, Level] = Map.empty

  def configuredClassLevel(cls: Class[_]): Option[Level] = {
    val name = getClassName(cls)
    findValueByKey(classNameLevels)(name.startsWith)
  }

  // event  root   class  || keep
  // -      error  -      || true   => !event.isDefined
  // -      info   -      || true
  // -      debug  -      || true
  // info   error  -      || false  => root <= event
  // info   info   -      || true
  // info   debug  -      || true
  // info   error  error  || false  => class <= event
  // info   info   error  || false
  // info   debug  error  || false
  // info   error  info   || true   => class <= event
  // info   info   info   || true
  // info   debug  info   || true
  def forwards(event: LogEvent[E]): Boolean = {
    def getPrimitive[I](key: LogKey[I, E]): Option[Primitive[I]] =
      event.get(key).map(_.asInstanceOf[LogPrimitive[I, E]])
    // TODO: avoid asInstanceOf if possible without shapeless
    def evaluatedPrimitive[I](key: Key[I]): Option[I] = getPrimitive(key).map(_.evaluated)
    val eventLevel: Level = evaluatedPrimitive(predefKeys.Level).getOrElse(Level.Highest)
    val configLevel: Level = evaluatedPrimitive(predefKeys.Logger).flatMap(configuredClassLevel).getOrElse(rootLevel)
    configLevel forwards eventLevel
  }
}
