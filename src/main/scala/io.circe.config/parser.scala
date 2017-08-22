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

import cats.data.ValidatedNel
import cats.syntax.either._
import java.io.File
import scala.collection.JavaConverters._
import com.typesafe.config._

/**
 * Utilities for parsing
 * [[https://typesafehub.github.io/config/latest/api/com/typesafe/config/Config.html com.typesafe.config.Config]]
 * sources to [[io.circe.Json]] as well as decoding to a specific type.
 *
 * @example
 * {{{
 * scala> import com.typesafe.config.ConfigFactory
 * scala> import io.circe.config.parser
 * scala> val config = ConfigFactory.parseString("server { host = localhost, port = 8080 }")
 *
 * scala> val json: Either[io.circe.ParsingFailure, io.circe.Json] = parser.parse(config)
 * scala> json.right.get.noSpaces
 * res0: String = {"server":{"port":8080,"host":"localhost"}}
 *
 * scala> import io.circe.generic.auto._
 * scala> case class ServerSettings(host: String, port: Int)
 * scala> parser.decode[ServerSettings](config.getConfig("server"))
 * res1: Either[io.circe.Error, ServerSettings] = Right(ServerSettings(localhost,8080))
 * }}}
 *
 * @see [[syntax.configDecoder]] for how to map [[io.circe.Json]] to
 * [[https://typesafehub.github.io/config/latest/api/com/typesafe/config/Config.html com.typesafe.config.Config]]
 */
object parser extends Parser {

  private[this] final def toJson[T](parseConfig: => Config): Either[ParsingFailure, Json] = {
    def convertValueUnsafe(value: ConfigValue): Json = value match {
      case obj: ConfigObject =>
        Json.fromFields(obj.asScala.mapValues(convertValueUnsafe))

      case list: ConfigList =>
        Json.fromValues(list.asScala.map(convertValueUnsafe))

      case scalar =>
        (value.valueType, value.unwrapped) match {
          case (ConfigValueType.NULL, _) =>
            Json.Null
          case (ConfigValueType.NUMBER, int: java.lang.Integer) =>
            Json.fromInt(int)
          case (ConfigValueType.NUMBER, long: java.lang.Long) =>
            Json.fromLong(long)
          case (ConfigValueType.NUMBER, double: java.lang.Double) =>
            Json.fromDouble(double).getOrElse {
              throw new NumberFormatException(s"Invalid numeric string ${value.render}")
            }
          case (ConfigValueType.BOOLEAN, boolean: java.lang.Boolean) =>
            Json.fromBoolean(boolean)
          case (ConfigValueType.STRING, str: String) =>
            Json.fromString(str)

          case (valueType, _) =>
            throw new RuntimeException(s"No conversion for $valueType with value $value")
        }
    }

    Either
      .catchNonFatal(convertValueUnsafe(parseConfig.root))
      .leftMap(error => ParsingFailure(error.getMessage, error))
  }

  final def parse(config: Config): Either[ParsingFailure, Json] =
    toJson(config)

  final def parse(input: String): Either[ParsingFailure, Json] =
    toJson(ConfigFactory.parseString(input))

  final def parseFile(file: File): Either[ParsingFailure, Json] =
    toJson(ConfigFactory.parseFile(file))

  final def decode[A: Decoder](config: Config): Either[Error, A] =
    finishDecode(parse(config))

  final def decodeFile[A: Decoder](file: File): Either[Error, A] =
    finishDecode[A](parseFile(file))

  final def decodeAccumulating[A: Decoder](config: Config): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parse(config))

  final def decodeFileAccumulating[A: Decoder](file: File): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseFile(file))

}
