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

import com.typesafe.config._
import io.circe._

object syntax {

  implicit class CirceConfigOps(val config: Config) extends AnyVal {
    /**
     * Read config settings into the specified type.
     */
    def as[A: Decoder]: Either[io.circe.Error, A] =
      parser.decode[A](config)

    /**
     * Read config settings at given path into the specified type.
     */
    def as[A: Decoder](path: String): Either[io.circe.Error, A] =
      if (config.hasPath(path)) parser.decode[A](config.getConfig(path))
      else Left(ParsingFailure("Path not found in config", new ConfigException.Missing(path)))
  }
}
