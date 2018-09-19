package de.kaufhof.ets.logging.split.syntax

trait LogEncoderOps[E] extends LogTypeDefinitions[E] {
  class EncoderValueHalfApplied[A] {
    def by[B](f: A => B)(implicit derivedFrom: Encoder[B]): Encoder[A] =
      value => derivedFrom.encode(f(value))
  }
  def apply[A]: EncoderValueHalfApplied[A] = new EncoderValueHalfApplied[A]
  def fromImplicit[A: Encoder]: Encoder[A] = Predef.implicitly
  def fromToString[A](implicit e: Encoder[String]): Encoder[A] = apply[A].by[String](_.toString)
}
