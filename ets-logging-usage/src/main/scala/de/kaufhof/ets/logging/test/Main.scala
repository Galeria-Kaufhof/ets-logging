package de.kaufhof.ets.logging.test

import java.time.Instant

import cats.effect.IO
import de.kaufhof.ets.logging.test.domain._
import org.slf4j.LoggerFactory

import scala.util.Random

object Main extends App {
  val jsonConfig = encoding.playjson.JsonLogConfig
  val catsioConfig = encoding.catsio.CatsIoJsonLogConfig
  val actorConfig = encoding.actor.JsonLogConfig

  object README extends encoding.string.StringLogConfig.LogInstance {
    // use standard log methods with severity for the log level and a message
    log.debug("test123")
    log.info("test234")

    // provide additional information with an arbitrary amount of key value pairs, called attributes
    import encoding.string.StringLogConfig.syntax._
    log.error("test345", Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid)
    // use the generic event method to construct arbitrary log events without any predefined attributes
    log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.Timestamp ~> Instant.MIN)
    // inputs to encoders are contravariant and therefore directly accept instances of the key-types's subtypes
    log.info("""yay \o/""", Keys.Epic -> Epic.FeatureA)

    // or pass any amount of decomposable objects
    // this requires an implicit decomposer to be in scope
    // then the decomposer will decompose the available attributes for you
    import encoding.string.StringLogConfig.syntax.decomposers._
    log.event(variant)
    // inputs to decomposers are contravariant as well and therefore accept instances of the input-types's subtypes
    log.info("""yay \o/""",  Epic.FeatureA)
  }

  object Slf4j extends encoding.slf4j.StringLogConfig.LogInstance {
    log.debug("test123")
    log.info("test234")

    LoggerFactory.getLogger("test").info("Log with slf4j")
  }

  object Configurable extends encoding.configurable.TupledLogConfig.LogInstance {
    log.debug("test-configurable")
    log.info("test-configurable")
  }

  object Test extends encoding.playjson.JsonLogConfig.LogInstance {
    import encoding.playjson.JsonLogConfig.syntax._
    import encoding.playjson.JsonLogConfig.syntax.decomposers._
    log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.Timestamp ~> Instant.MIN)
    log.event(variant)
    log.debug("test123", variant)
    log.info("test234", variant)
    log.error("test345", variant)
  }
  object Asdf extends jsonConfig.LogInstance {
    import jsonConfig.syntax._
    import jsonConfig.syntax.decomposers._
    log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.Timestamp ~> Instant.MIN)
    log.event(variant)
    log.debug("test123", variant)
    log.info("test234", variant)
    log.error("test345", variant)
  }

  object TestEager extends jsonConfig.LogInstance {
    import jsonConfig.syntax._
    val rEval = new Random(0)
    val rEnc = new Random(0)
    def eval: jsonConfig.Attribute = Keys.RandomEval -> rEval.nextInt(100)
    def enc: jsonConfig.Attribute = Keys.RandomEncoder -> rEnc
    log.event(eval, enc)
    log.event(eval, enc)
    log.event(eval, enc)
  }

  object TestLazy extends jsonConfig.LogInstance {
    import jsonConfig.syntax._
    val rEval = new Random(0)
    val rEnc = new Random(0)
    val eval: jsonConfig.Attribute = Keys.RandomEval ~> rEval.nextInt(100)
    val enc: jsonConfig.Attribute = Keys.RandomEncoder ~> rEnc
    log.event(eval, enc)
    log.event(eval, enc)
    log.event(eval, enc)
  }

  object CatsIoTest extends catsioConfig.LogInstance {
    import catsioConfig.syntax._
    import catsioConfig.syntax.decomposers._
    val x: IO[Unit] = for {
      _ <- log.event(Keys.VariantId ~> variant.id, Keys.SomeUUID -> uuid, Keys.Timestamp ~> Instant.MIN)
      _ <- log.event(variant)
      _ <- log.debug("test123", variant)
      _ <- log.info("test234", variant)
      _ <- log.error("test345", variant)
    } yield ()
    println
    println("nothing happened so far")
    x.unsafeRunSync()
    println("execution done!")
  }

  object ActorTest extends actorConfig.LogInstance {
    import actorConfig.syntax.decomposers._
    import akka.actor._
    val as = ActorSystem("test")
    as.actorOf(Props(new SomeActor), "somesource")
    class SomeActor extends Actor {
      val x: IO[Unit] = for {
        _ <- log.event(this)
        _ <- log.event(variant)
        _ <- log.debug("test123", variant)
        _ <- log.info("test234", variant)
        _ <- log.error("test345", variant)
      } yield ()
      println
      println("nothing happened so far")
      x.unsafeRunSync()
      println("execution done!")

      context.stop(self)
      as.terminate()
      override def receive: Receive = {
        case _ =>
      }
    }
  }

  README
  Slf4j
  Configurable
  Test
  Asdf
  println
  TestEager
  TestLazy
  CatsIoTest
  ActorTest
}
