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
import com.typesafe.config.{parser => _, _}
import cats.instances.either._
import cats.syntax.either._
import cats.syntax.bifunctor._

import scala.concurrent.duration._
import java.time.Period

/**
 * Implicits for decoding Typesafe Config values and instances using
 * [[io.circe.Decoder circe decoders]].
 *
 * In addition to [[syntax.durationDecoder]] and [[syntax.memorySizeDecoder]]
 * for reading Typesafe Config specific value formats, this module also provides
 * [[syntax.CirceConfigOps]] for decoding loaded configurations.
 *
 * @example
 * {{{
 * scala> import io.circe.generic.auto._
 * scala> import io.circe.config.syntax._
 * scala> import scala.concurrent.duration.FiniteDuration
 * scala> case class ServerSettings(port: Int, host: String, timeout: FiniteDuration)
 * scala> val config = com.typesafe.config.ConfigFactory.parseString("port = 7357, host = localhost, timeout = 5 s")
 * scala> config.as[ServerSettings]
 * res0: Either[io.circe.Error, ServerSettings] = Right(ServerSettings(7357,localhost,5 seconds))
 * }}}
 */
object syntax {
  /**
   * Decoder for reading
   * [[https://github.com/lightbend/config/blob/master/HOCON.md#duration-format duration format]].
   *
   * @example
   * {{{
   * scala> import io.circe.Json
   * scala> import io.circe.config.syntax._
   * scala> import scala.concurrent.duration.FiniteDuration
   *
   * scala> durationDecoder.decodeJson(Json.fromString("5 seconds"))
   * res0: io.circe.Decoder.Result[FiniteDuration] = Right(5 seconds)
   * scala> durationDecoder.decodeJson(Json.fromString("1 hour"))
   * res1: io.circe.Decoder.Result[FiniteDuration] = Right(1 hour)
   *
   * scala> Json.fromString("200 ms").as[FiniteDuration]
   * res2: io.circe.Decoder.Result[FiniteDuration] = Right(200 milliseconds)
   * }}}
   */
  implicit val durationDecoder: Decoder[FiniteDuration] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal {
      val d = ConfigValueFactory.fromAnyRef(str).atKey("d").getDuration("d")
      Duration.fromNanos(d.toNanos)
    }.leftMap(t => "Decoder[FiniteDuration]")
  }

  /**
   * Decoder for reading
   * [[https://github.com/lightbend/config/blob/master/HOCON.md#period-format period format]].
   *
   * @example
   * {{{
   * scala> import io.circe.Json
   * scala> import io.circe.config.syntax._
   * scala> import java.time.Period
   *
   * scala> periodDecoder.decodeJson(Json.fromString("1 day"))
   * res0: io.circe.Decoder.Result[Period] = Right(P1D)
   * scala> periodDecoder.decodeJson(Json.fromString("3 y"))
   * res1: io.circe.Decoder.Result[Period] = Right(P3Y)
   *
   * scala> Json.fromString("24 months").as[Period]
   * res2: io.circe.Decoder.Result[Period] = Right(P24M)
   * }}}
   */
  implicit val periodDecoder: Decoder[Period] = Decoder.decodeString.emap { str =>
    Either
      .catchNonFatal(ConfigValueFactory.fromAnyRef(str).atKey("p").getPeriod("p"))
      .leftMap(t => "Decoder[Period]")
  }

  /**
   * Decoder for reading
   * [[https://github.com/lightbend/config/blob/master/HOCON.md#size-in-bytes-format size in bytes format]]
   * into a
   * [[https://lightbend.github.io/config/latest/api/com/typesafe/config/ConfigMemorySize.html com.typesafe.config.ConfigMemorySize]].
   *
   * @example
   * {{{
   * scala> import io.circe.Json
   * scala> import io.circe.config.syntax._
   * scala> import com.typesafe.config.ConfigMemorySize
   *
   * scala> memorySizeDecoder.decodeJson(Json.fromString("128M"))
   * res0: io.circe.Decoder.Result[ConfigMemorySize] = Right(ConfigMemorySize(134217728))
   * scala> memorySizeDecoder.decodeJson(Json.fromString("4096 KiB"))
   * res1: io.circe.Decoder.Result[ConfigMemorySize] = Right(ConfigMemorySize(4194304))
   *
   * scala> Json.fromString("32 GB").as[ConfigMemorySize]
   * res2: io.circe.Decoder.Result[ConfigMemorySize] = Right(ConfigMemorySize(32000000000))
   * }}}
   */
  implicit val memorySizeDecoder: Decoder[ConfigMemorySize] = Decoder.decodeString.emap { str =>
    Either
      .catchNonFatal(ConfigValueFactory.fromAnyRef(str).atKey("m").getMemorySize("m"))
      .leftMap(t => "Decoder[ConfigMemorySize]")
  }

  /**
   * Decoder for converting [[io.circe.Json]] to
   * [[https://lightbend.github.io/config/latest/api/com/typesafe/config/ConfigValue.html com.typesafe.config.ConfigValue]].
   *
   * Maps any circe JSON AST to the Typesafe Config AST.
   *
   * @example
   * {{{
   * scala> import io.circe.Json
   * scala> import com.typesafe.config.ConfigValue
   * scala> import io.circe.config.syntax._
   *
   * scala> val hostJson = Json.fromString("localhost")
   * scala> val portJson = Json.fromInt(8080)
   * scala> val serverJson = Json.obj("host" -> hostJson, "port" -> portJson)
   *
   * scala> configValueDecoder.decodeJson(hostJson)
   * res0: io.circe.Decoder.Result[ConfigValue] = Right(Quoted("localhost"))
   *
   * scala> configValueDecoder.decodeJson(portJson)
   * res1: io.circe.Decoder.Result[ConfigValue] = Right(ConfigLong(8080))
   *
   * scala> serverJson.as[ConfigValue]
   * res2: io.circe.Decoder.Result[ConfigValue] = Right(SimpleConfigObject({"host":"localhost","port":8080}))
   * }}}
   *
   * @see [[configDecoder]] for decoding circe JSON objects to a Typesafe Config instance.
   */
  implicit val configValueDecoder: Decoder[ConfigValue] = Decoder.decodeJson.emap { json =>
    Either.catchNonFatal(jsonToConfigValue(json)).leftMap(t => "Decoder[ConfigValue]")
  }

  /**
   * Decoder for converting [[io.circe.Json]] to
   * [[https://lightbend.github.io/config/latest/api/com/typesafe/config/Config.html com.typesafe.config.Config]].
   *
   * Converts a circe JSON object to a Typesafe Config instance.
   *
   * @example
   * {{{
   * scala> import io.circe.Json
   * scala> import com.typesafe.config.Config
   * scala> import io.circe.config.syntax._
   *
   * scala> val hostJson = Json.fromString("localhost")
   * scala> val portJson = Json.fromInt(8080)
   * scala> val serverJson = Json.obj("host" -> hostJson, "port" -> portJson)
   *
   * scala> configDecoder.decodeJson(Json.obj("host" -> hostJson))
   * res0: io.circe.Decoder.Result[Config] = Right(Config(SimpleConfigObject({"host":"localhost"})))
   * scala> serverJson.as[Config]
   * res1: io.circe.Decoder.Result[Config] = Right(Config(SimpleConfigObject({"host":"localhost","port":8080})))
   *
   * scala> portJson.as[Config]
   * res2: io.circe.Decoder.Result[Config] = Left(DecodingFailure(JSON must be an object, was type NUMBER, List()))
   * }}}
   *
   * @see [[configValueDecoder]] for decoding any circe JSON AST.
   */
  implicit val configDecoder: Decoder[Config] = configValueDecoder.emap { value =>
    value match {
      case obj: ConfigObject => Right(obj.toConfig)
      case other => Left(s"JSON must be an object, was type ${other.valueType}")
    }
  }

  /**
   * Enriches
   * [[https://lightbend.github.io/config/latest/api/com/typesafe/config/Config.html com.typesafe.config.Config]]
   * instances with methods to decode to a specific type.
   */
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
      parser.decodePath[A](config, path)

    /**
      * Read config settings into the specified type.
      */
    def asF[F[_], A: Decoder](implicit ev: ApplicativeError[F, Throwable]): F[A] =
      parser.decodeF[F, A](config)

    /**
      * Read config settings at given path into the specified type.
      */
    def asF[F[_], A: Decoder](path: String)(implicit ev: ApplicativeError[F, Throwable]): F[A] =
      parser.decodePathF[F, A](config, path)
  }
}
