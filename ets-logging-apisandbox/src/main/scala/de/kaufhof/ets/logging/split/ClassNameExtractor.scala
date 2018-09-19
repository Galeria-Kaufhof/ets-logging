package de.kaufhof.ets.logging.split

trait ClassNameExtractor {
  def getClassName(cls: Class[_]): String = cls.getSimpleName
}
