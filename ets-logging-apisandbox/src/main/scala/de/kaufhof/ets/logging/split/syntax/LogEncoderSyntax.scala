package de.kaufhof.ets.logging.split.syntax

trait LogEncoderSyntax[E] extends LogTypeDefinitions[E] {
  def encodeString(string: String): Encoded
  final implicit lazy val stringEncoder: Encoder[String] = encodeString
  object Encoder extends LogEncoderOps[Encoded]
}
