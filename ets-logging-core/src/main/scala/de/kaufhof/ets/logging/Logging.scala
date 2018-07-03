package de.kaufhof.ets.logging


trait SLF4J {
  import org.slf4j.LoggerFactory
  val logger = LoggerFactory.getLogger("foo")
}

case class AttributeKey(name: String) extends AnyVal
case class Attribute[T](key: AttributeKey, value: T)

object GlobalAttributeKeys {
  val throwable: AttributeKey = AttributeKey("throwable")
  val timestamp: AttributeKey = AttributeKey("timestamp")
}

trait Logger {

  import GlobalAttributeKeys.throwable

  def trace(msg: Symbol, kvs: Attribute[_]*)
  def debug(msg: Symbol, kvs: Attribute[_]*)
  def info(msg: Symbol, kvs: Attribute[_]*)
  def warn(msg: Symbol, kvs: Attribute[_]*)
  def error(msg: Symbol, kvs: Attribute[_]*)

  def warn(msg: Symbol, throwable: Throwable, kvs: Attribute[_]*)
  def error(msg: Symbol, throwable: Throwable, kvs: Attribute[_]*)
  // TODO more for throwables?
}

// the trait to be mixed in
trait LogInstance {
  def log: Logger = ???

  def extractAndFoldAttributes(existingAttributes: Seq[Attribute[_]]): Seq[Attribute[_]]


  def registerLogEnricher(): Unit = {

  }
}

trait AttributeExtractor[C] extends LogInstance {

  def extractAttributes(c: C): Seq[Attribute[_]]

  /**
    * Scaladoc here describing that you need to check the class name (lazy loading for optional deps..)
    * @return
    */
  def isEnabled: Boolean
  def getContext: C

  override def extractAndFoldAttributes(existingAttributes: Seq[Attribute[_]]): Seq[Attribute[_]] = {
    val extractedAttributes = extractAttributes(getContext)
    super.extractAndFoldAttributes(existingAttributes ++ extractedAttributes)
  }

}

trait CommonAttributeExtractor[T] extends AttributeExtractor[T] {
  lazy val a: Int
  override def extractAttributes(c: T): Seq[Attribute[_]] = {

  }
}

trait AkkaAttributeExtractor[A: Actor] extends AttributeExtractor[A] {

  override final val isEnabled = getClass.getCanonicalName == "akka.Actor"

  override def extractAttributes(): Seq[Attribute[_]] = {

  }

}

trait Actor

trait



class Main {

}
