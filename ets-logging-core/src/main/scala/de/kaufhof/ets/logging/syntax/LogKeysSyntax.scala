package de.kaufhof.ets.logging.syntax

import de.kaufhof.ets.logging.ClassNameExtractor
import de.kaufhof.ets.logging.generic.LogPredefKeys

trait LogKeysSyntax[E]
    extends LogPredefKeys[E]
    with LogTypeDefinitions[E]
    with LogKeySyntax[E]
    with LogEncoderSyntax[E]
    with ClassNameExtractor
