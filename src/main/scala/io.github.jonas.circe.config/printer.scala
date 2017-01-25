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
package io.github.jonas.circe.config

import io.circe._
import com.typesafe.config._

object printer {

  val defaultOptions = ConfigRenderOptions.defaults.setJson(false).setOriginComments(false)

  /**
   * Print JSON to a Typesafe Config string.
   */
  def print(root: Json, options: ConfigRenderOptions = defaultOptions): String = {
    val origin = ConfigOriginFactory.newSimple("circe-config printer")

    jsonToConfigValue(root).render(options)
  }

}
