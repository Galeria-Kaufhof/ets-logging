package de.kaufhof.ets.logging.split

trait LogAttributeProcessor[E, O] {
  def process(attributes: Seq[generic.LogAttribute[E]]): O
}
