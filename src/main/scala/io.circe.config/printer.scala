/*
 * Copyright 2017 Jonas Fonseca
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.circe
package config

import com.typesafe.config._

/**
 * Print [[io.circe.Json]] to a Typesafe Config string.
 *
 * @example
 * {{{
 * scala> import io.circe.Json
 * scala> import io.circe.config.printer
 *
 * scala> val options = printer.DefaultOptions.setFormatted(false)
 * scala> val json = Json.obj("server" -> Json.obj("host" -> Json.fromString("localhost"), "port" -> Json.fromInt(8080)))
 * scala> printer.print(json, options)
 * res0: String = server{host=localhost,port=8080}
 * }}}
 */
object printer {

  /**
   * Default printer options.
   *
   * By default JSON is printed in
   * [[https://github.com/typesafehub/config/blob/master/HOCON.md HOCON format]]
   * without origin comments.
   *
   * @see [[https://typesafehub.github.io/config/latest/api/com/typesafe/config/ConfigRenderOptions.html com.typesafe.config.ConfigRenderOptions]]
   */
  val DefaultOptions = ConfigRenderOptions.defaults.setJson(false).setOriginComments(false)

  /**
   * Print JSON to a Typesafe Config string.
   *
   * @param root A [[io.circe.JsonObject JSON object]]
   * @param options Printer options allowing to configure printing to JSON or HOCON
   */
  def print(root: Json, options: ConfigRenderOptions = DefaultOptions): String =
    jsonToConfigValue(root).render(options)

}
