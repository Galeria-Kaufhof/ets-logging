package de.kaufhof.ets.logging

trait ClassNameExtractor {
  def getClassName(cls: Class[_]): String = cls.getSimpleName
}
