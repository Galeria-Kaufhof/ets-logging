package de.kaufhof.ets.logging.split

package object util {
  def findValueByKey[A, B](m: Map[A, B])(p: A => Boolean): Option[B] = m.collectFirst {
    case (pattern, value) if p(pattern) => value
  }
  type Lazy[A] = () => A
  object Lazy {
    def apply[A](value: => A): Lazy[A] = value _
  }
}
