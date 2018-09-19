package de.kaufhof.ets.logging.split.generic

trait LogEncoder[I, O] { def encode(value: I): O }