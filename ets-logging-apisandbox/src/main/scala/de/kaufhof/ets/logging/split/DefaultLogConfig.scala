package de.kaufhof.ets.logging.split

import de.kaufhof.ets.logging.split.syntax._

trait DefaultLogConfig[E, O]
  extends LogKeySyntax[E]
    with LoggerFactory[E, O]
    with DefaultLoggerFactory[E, O]
    with AbstractLogAttributeProcessor[E, O]
    with ClassLevelLogEventFilter[E]
    with DefaultEncoders[E]
    with DefaultAttributeGatherer[E]
    with ClassNameExtractor
