package de.kaufhof.ets.logging

import de.kaufhof.ets.logging.generic._
import de.kaufhof.ets.logging.syntax._

trait DefaultLoggerFactory[E, O]
    extends LoggerFactory[E, O]
    with PredefKeysInstance[E]
    with LogAttributeProcessor[E, O] {
  def createLogger(cls: Class[_]): Logger = new Logger {
    private lazy val clsAttr = predefKeys.Logger -> cls
    private def generic(attributes: Seq[LogAttribute[E]]): Output = process(attributes :+ clsAttr)
    final override def event(attrs: LogAttribute[E]*): Output = generic(attrs)
    final override def trace(msg: String, attrs: LogAttribute[Encoded]*): Output =
      generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Trace)
    final override def debug(msg: String, attrs: LogAttribute[E]*): Output =
      generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Debug)
    final override def info(msg: String, attrs: LogAttribute[E]*): Output =
      generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Info)
    final override def warn(msg: String, attrs: LogAttribute[Encoded]*): Output =
      generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Warn)
    final override def error(msg: String, attrs: LogAttribute[E]*): Output =
      generic(attrs :+ predefKeys.Message ~> msg :+ predefKeys.Level ~> Level.Error)
  }
}
