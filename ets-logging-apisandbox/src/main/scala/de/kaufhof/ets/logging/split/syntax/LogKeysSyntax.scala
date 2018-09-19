package de.kaufhof.ets.logging.split.syntax

import de.kaufhof.ets.logging.split.ClassNameExtractor
import de.kaufhof.ets.logging.split.generic.LogPredefKeys

trait LogKeysSyntax[E]
    extends LogPredefKeys[E]
    with LogTypeDefinitions[E]
    with LogKeySyntax[E]
    with LogEncoderSyntax[E]
    with ClassNameExtractor
