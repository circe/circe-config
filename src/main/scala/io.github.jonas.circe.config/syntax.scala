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
import cats.syntax.either._
import scala.concurrent.duration._

/**
 * Implicits for decoding Typesafe Config instances using circe
 * [[io.circe.Decoder decoders]].
 *
 * In addition to [[syntax.durationDecoder]] and [[syntax.memorySizeDecoder]]
 * for reading Typesafe Config specific value formats, this module also provides
 * [[syntax.CirceConfigOps]] for decoding loaded configurations.
 *
 * @example
 * {{{
 * scala> import io.circe.generic.auto._
 * scala> import io.github.jonas.circe.config.syntax._
 * scala> case class Server(port: Int, host: String)
 * scala> val config = com.typesafe.config.ConfigFactory.parseString("port = 7357, host = localhost")
 * scala> config.as[Server]
 * res0: Either[io.circe.Error, Server] = Right(Server(7357,localhost))
 * }}}
 */
object syntax {
  /**
   * Decoder for reading
   * [[https://github.com/typesafehub/config/blob/master/HOCON.md#duration-format duration formats]].
   */
  implicit val durationDecoder: Decoder[FiniteDuration] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal {
      val d = ConfigValueFactory.fromAnyRef(str).atKey("d").getDuration("d")
      Duration.fromNanos(d.toNanos)
    }.leftMap(t => "Decoder[FiniteDuration]")
  }

  /**
   * Decoder for reading
   * [[https://github.com/typesafehub/config/blob/master/HOCON.md#size-in-bytes-format memory size in bytes format]].
   */
  implicit val memorySizeDecoder: Decoder[ConfigMemorySize] = Decoder.decodeString.emap { str =>
    Either
      .catchNonFatal(ConfigValueFactory.fromAnyRef(str).atKey("m").getMemorySize("m"))
      .leftMap(t => "Decoder[ConfigMemorySize]")
  }

  /**
   * Decoder for converting [[io.circe.Json]] to Typesafe Config values.
   *
   * Maps from any circe's JSON AST to the Typesafe Config AST.
   *
   * @example
   * {{{
   * scala> import io.circe.Json
   * scala> import com.typesafe.config.ConfigValue
   * scala> import io.github.jonas.circe.config.syntax._
   *
   * scala> val hostJson = Json.fromString("localhost")
   * scala> val portJson = Json.fromInt(8080)
   * scala> val serverJson = Json.obj("host" -> hostJson, "port" -> portJson)
   *
   * scala> hostJson.as[ConfigValue]
   * res0: io.circe.Decoder.Result[ConfigValue] = Right(Quoted("localhost"))
   *
   * scala> portJson.as[ConfigValue]
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
   * Decoder for converting [[io.circe.Json]] to a Typesafe Config instance.
   *
   * Converts a circe JSON object to a Typesafe Config instance.
   *
   * @example
   * {{{
   * scala> import io.circe.Json
   * scala> import com.typesafe.config.Config
   * scala> import io.github.jonas.circe.config.syntax._
   *
   * scala> val hostJson = Json.fromString("localhost")
   * scala> val portJson = Json.fromInt(8080)
   * scala> val serverJson = Json.obj("host" -> hostJson, "port" -> portJson)
   *
   * scala> portJson.as[Config]
   * res0: io.circe.Decoder.Result[Config] = Left(DecodingFailure(JSON must be an object, was type NUMBER, List()))
   *
   * scala> serverJson.as[Config]
   * res3: io.circe.Decoder.Result[Config] = Right(Config(SimpleConfigObject({"host":"localhost","port":8080})))
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
   * Enriches Typesafe Config instances with methods to decode to a specific type.
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
      if (config.hasPath(path)) parser.decode[A](config.getConfig(path))
      else Left(ParsingFailure("Path not found in config", new ConfigException.Missing(path)))
  }
}
