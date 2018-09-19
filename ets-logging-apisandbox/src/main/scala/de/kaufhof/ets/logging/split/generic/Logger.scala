package de.kaufhof.ets.logging.split.generic

trait Logger[E, O] {
  def event(attrs: LogAttribute[E]*): O
  def trace(msg: String, attrs: LogAttribute[E]*): O
  def debug(msg: String, attrs: LogAttribute[E]*): O
  def info(msg: String, attrs: LogAttribute[E]*): O
  def warn(msg: String, attrs: LogAttribute[E]*): O
  def error(msg: String, attrs: LogAttribute[E]*): O
}
