package io.circe.config

// Probably not needed after https://github.com/scala/scala-collection-compat/pull/217
package object compat {
  val converters = scala.jdk.CollectionConverters
}
