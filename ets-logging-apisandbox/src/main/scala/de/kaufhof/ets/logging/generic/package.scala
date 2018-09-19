package de.kaufhof.ets.logging

package object generic {
  type LogEvent[E] = Map[LogKey[_, E], LogPrimitive[_, E]]
}
