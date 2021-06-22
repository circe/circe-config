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

import cats.ApplicativeError
import cats.data.ValidatedNel
import cats.syntax.either._
import cats.syntax.bifunctor._
import cats.instances.either._
import java.io.File

import compat.converters._
import com.typesafe.config._

/**
 * Utilities for parsing
 * [[https://lightbend.github.io/config/latest/api/com/typesafe/config/Config.html com.typesafe.config.Config]]
 * sources to [[io.circe.Json]] as well as decoding to a specific type.
 *
 * If you are working in something like [[https://typelevel.org/cats-effect/api/cats/effect/IO cats.effect.IO]], or some other type `F[_]` that provides a
 * [[cats.ApplicativeError]], there're also decoders for loading such types.
 *
 * @example
 * {{{
 * scala> import com.typesafe.config.ConfigFactory
 * scala> import io.circe.config.parser
 * scala> val config = ConfigFactory.parseString("server { host = localhost, port = 8080 }")
 *
 * scala> val json: Either[io.circe.ParsingFailure, io.circe.Json] = parser.parse(config)
 * scala> json.map(_.noSpaces).getOrElse("Parse failure")
 * res0: String = {"server":{"port":8080,"host":"localhost"}}
 *
 * scala> import io.circe.generic.auto._
 * scala> case class ServerSettings(host: String, port: Int)
 * scala> case class AppSettings(server: ServerSettings)
 *
 * scala> parser.decode[AppSettings](config)
 * res1: Either[io.circe.Error, AppSettings] = Right(AppSettings(ServerSettings(localhost,8080)))
 * scala> parser.decodePath[ServerSettings](config, "server")
 * res2: Either[io.circe.Error, ServerSettings] = Right(ServerSettings(localhost,8080))
 *
 * scala> import cats.effect.IO
 * scala> parser.decodePathF[IO, ServerSettings](config, "server")
 * res3: cats.effect.IO[ServerSettings] = IO(ServerSettings(localhost,8080))
 * }}}
 *
 * @see [[syntax.configDecoder]] for how to map [[io.circe.Json]] to
 * [[https://lightbend.github.io/config/latest/api/com/typesafe/config/Config.html com.typesafe.config.Config]]
 */
object parser extends Parser {

  private[this] final def toJson(parseConfig: => Config, path: Option[String] = None): Either[ParsingFailure, Json] = {
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

    Either.catchNonFatal {
      convertValueUnsafe {
        val config = parseConfig
        path
          .fold(config) { path =>
            if (config.hasPath(path)) config.getConfig(path)
            else throw new ParsingFailure("Path not found in config", new ConfigException.Missing(path))
          }
          .root
      }
    }.leftMap(error => ParsingFailure(error.getMessage, error))
  }

  final def parse(): Either[ParsingFailure, Json] =
    toJson(ConfigFactory.load())

  final def parse(config: Config): Either[ParsingFailure, Json] =
    toJson(config)

  final def parse(input: String): Either[ParsingFailure, Json] =
    toJson(ConfigFactory.parseString(input))

  final def parseFile(file: File): Either[ParsingFailure, Json] =
    toJson(ConfigFactory.parseFile(file))

  final def parsePath(path: String): Either[ParsingFailure, Json] =
    toJson(ConfigFactory.load(), Some(path))

  final def parsePath(config: Config, path: String): Either[ParsingFailure, Json] =
    toJson(config, Some(path))

  /**
   * Load the default configuration and decode an instance at a specific path.
   *
   * @example
   * {{{
   * scala> import io.circe.generic.auto._
   * scala> case class ServerSettings(host: String, port: Int)
   * scala> case class HttpSettings(server: ServerSettings)
   * scala> case class AppSettings(http: HttpSettings)
   *
   * scala> parser.decode[AppSettings]()
   * res0: Either[io.circe.Error, AppSettings] = Right(AppSettings(HttpSettings(ServerSettings(localhost,8080))))
   * }}}
   */
  final def decode[A: Decoder](): Either[Error, A] =
    finishDecode(parse())

  /**
   * Load the default configuration and decode an instance at a specific path.
   *
   * @example
   * {{{
   * scala> import io.circe.generic.auto._
   * scala> case class ServerSettings(host: String, port: Int)
   * scala> case class HttpSettings(server: ServerSettings)
   * scala> case class AppSettings(http: HttpSettings)
   *
   * scala> import com.typesafe.config.ConfigFactory
   * scala> val config = ConfigFactory.load()
   *
   * scala> parser.decode[AppSettings](config)
   * res0: Either[io.circe.Error, AppSettings] = Right(AppSettings(HttpSettings(ServerSettings(localhost,8080))))
   * }}}
   */
  final def decode[A: Decoder](config: Config): Either[Error, A] =
    finishDecode(parse(config))

  /**
   * Load configuration from file and decode an instance.
   *
   * @example
   * {{{
   * scala> import io.circe.generic.auto._
   * scala> case class ServerSettings(host: String, port: Int)
   * scala> case class HttpSettings(server: ServerSettings)
   * scala> case class AppSettings(http: HttpSettings)
   *
   * scala> parser.decodeFile[AppSettings](new java.io.File("src/test/resources/application.conf"))
   * res0: Either[io.circe.Error, AppSettings] = Right(AppSettings(HttpSettings(ServerSettings(localhost,8080))))
   * }}}
   */
  final def decodeFile[A: Decoder](file: File): Either[Error, A] =
    finishDecode(parseFile(file))

  /**
   * Load the default configuration and decode an instance.
   *
   * @example
   * {{{
   * scala> import io.circe.generic.auto._
   * scala> case class ServerSettings(host: String, port: Int)
   *
   * scala> parser.decodePath[ServerSettings]("http.server")
   * res0: Either[io.circe.Error, ServerSettings] = Right(ServerSettings(localhost,8080))
   * }}}
   */
  final def decodePath[A: Decoder](path: String): Either[Error, A] =
    finishDecode(parsePath(path))

  /**
   * Decode of an instance at a specific path.
   *
   * @example
   * {{{
   * scala> import io.circe.generic.auto._
   * scala> case class ServerSettings(host: String, port: Int)
   *
   * scala> import com.typesafe.config.ConfigFactory
   * scala> val config = ConfigFactory.load()
   *
   * scala> parser.decodePath[ServerSettings](config, "http.server")
   * res0: Either[io.circe.Error, ServerSettings] = Right(ServerSettings(localhost,8080))
   * }}}
   */
  final def decodePath[A: Decoder](config: Config, path: String): Either[Error, A] =
    finishDecode(parsePath(config, path))

  final def decodeAccumulating[A: Decoder](config: Config): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parse(config))

  final def decodeFileAccumulating[A: Decoder](file: File): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseFile(file))

  /**
   * Load default configuration and decode an instance supporting [[cats.ApplicativeError]].
   *
   * @example
   * {{{
   * scala> import io.circe.generic.auto._
   * scala> case class ServerSettings(host: String, port: Int)
   * scala> case class HttpSettings(server: ServerSettings)
   * scala> case class AppSettings(http: HttpSettings)
   *
   * scala> import cats.effect.IO
   * scala> parser.decodeF[IO, AppSettings]()
   * res0: cats.effect.IO[AppSettings] = IO(AppSettings(HttpSettings(ServerSettings(localhost,8080))))
   * }}}
   */
  final def decodeF[F[_], A: Decoder]()(implicit ev: ApplicativeError[F, Throwable]): F[A] =
    decode[A]().leftWiden[Throwable].liftTo[F]

  /**
   * Load default configuration and decode an instance supporting [[cats.ApplicativeError]] at a specific path.
   *
   * @example
   * {{{
   * scala> import io.circe.generic.auto._
   * scala> case class ServerSettings(host: String, port: Int)
   *
   * scala> import cats.effect.IO
   * scala> parser.decodePathF[IO, ServerSettings]("http.server")
   * res0: cats.effect.IO[ServerSettings] = IO(ServerSettings(localhost,8080))
   * }}}
   */
  final def decodePathF[F[_], A: Decoder](path: String)(implicit ev: ApplicativeError[F, Throwable]): F[A] =
    decodePath[A](path).leftWiden[Throwable].liftTo[F]

  /**
   * Decode an instance supporting [[cats.ApplicativeError]].
   *
   * @example
   * {{{
   * scala> import io.circe.generic.auto._
   * scala> case class ServerSettings(host: String, port: Int)
   * scala> case class HttpSettings(server: ServerSettings)
   * scala> case class AppSettings(http: HttpSettings)
   *
   * scala> import com.typesafe.config.ConfigFactory
   * scala> val config = ConfigFactory.load()
   *
   * scala> import cats.effect.IO
   * scala> parser.decodeF[IO, AppSettings](config)
   * res0: cats.effect.IO[AppSettings] = IO(AppSettings(HttpSettings(ServerSettings(localhost,8080))))
   * }}}
   */
  final def decodeF[F[_], A: Decoder](config: Config)(implicit ev: ApplicativeError[F, Throwable]): F[A] =
    decode[A](config).leftWiden[Throwable].liftTo[F]

  /**
   * Decode an instance supporting [[cats.ApplicativeError]] at a specific path.
   *
   * @example
   * {{{
   * scala> import io.circe.generic.auto._
   * scala> case class ServerSettings(host: String, port: Int)
   *
   * scala> import com.typesafe.config.ConfigFactory
   * scala> val config = ConfigFactory.load()
   *
   * scala> import cats.effect.IO
   * scala> import io.circe.config.parser
   * scala> parser.decodePathF[IO, ServerSettings](config, "http.server")
   * res0: cats.effect.IO[ServerSettings] = IO(ServerSettings(localhost,8080))
   * }}}
   */
  final def decodePathF[F[_], A: Decoder](config: Config, path: String)(implicit
    ev: ApplicativeError[F, Throwable]
  ): F[A] =
    decodePath[A](config, path).leftWiden[Throwable].liftTo[F]
}
