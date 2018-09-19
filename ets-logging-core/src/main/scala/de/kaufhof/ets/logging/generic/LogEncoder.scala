package de.kaufhof.ets.logging.generic

trait LogEncoder[I, O] { def encode(value: I): O }