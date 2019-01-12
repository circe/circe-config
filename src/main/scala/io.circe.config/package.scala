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

import cats.ApplicativeError
import cats.implicits._
import com.typesafe.config._
import scala.util.Try
import scala.collection.JavaConverters._
import config.syntax._

/**
 * circe-config: A [[https://github.com/lightbend/config Typesafe config]]
 * wrapper powered by [[io.circe circe]].
 *
 * @note '''Limitations for numerical types''':
 * Typesafe config uses Java's int, long and double types to represent numbers.
 * In some cases, double values may be represented internally as long after a
 * roundtrip since the [[https://github.com/lightbend/config/blob/master/HOCON.md HOCON]]
 * formatting is not stable. Also, precision may be lost when converting from
 * circe's [[io.circe.JsonNumber JsonNumber]] to Typesafe config's number
 * representation (as can be seen in the
 * [[https://github.com/circe/circe-config/blob/master/src/test/scala/io.circe.config/CirceConfigLaws.scala#L16-L40 test for the printer laws]]).
 *
 * @example
 * {{{
 * scala> import com.typesafe.config.ConfigFactory
 * scala> import io.circe.generic.auto._
 * scala> import io.circe.config.syntax._
 *
 * scala> case class ServerSettings(host: String, port: Int, ssl: Option[String])
 * scala> case class HttpSettings(server: ServerSettings, version: Double)
 * scala> case class AppSettings(http: HttpSettings)
 *
 * scala> val config = ConfigFactory.parseString("http { version = 1.1, server { host = localhost, port = 8080 } }")
 *
 * scala> config.as[ServerSettings]("http.server")
 * res0: Either[io.circe.Error, ServerSettings] = Right(ServerSettings(localhost,8080,None))
 *
 * scala> config.as[HttpSettings]("http")
 * res1: Either[io.circe.Error, HttpSettings] = Right(HttpSettings(ServerSettings(localhost,8080,None),1.1))
 *
 * scala> config.as[AppSettings]
 * res2: Either[io.circe.Error, AppSettings] = Right(AppSettings(HttpSettings(ServerSettings(localhost,8080,None),1.1)))
 * }}}
 */
package object config {
  def loadConfigF[F[_], C : Decoder](implicit ev : ApplicativeError[F, Throwable]) : F[C] =
    Either.fromTry(Try(ConfigFactory.load())).flatMap(_.as[C]).leftWiden[Throwable].raiseOrPure[F]

  def loadConfigF[F[_], C : Decoder](path : String)(implicit ev : ApplicativeError[F, Throwable]) : F[C] =
    Either.fromTry(Try(ConfigFactory.load())).flatMap(_.as[C](path)).leftWiden[Throwable].raiseOrPure[F]

  private[config] def jsonToConfigValue(json: Json): ConfigValue =
    json.fold(
      ConfigValueFactory.fromAnyRef(null),
      boolean => ConfigValueFactory.fromAnyRef(boolean),
      number => number.toLong match {
        case Some(long) => ConfigValueFactory.fromAnyRef(long)
        case None => ConfigValueFactory.fromAnyRef(number.toDouble)
      },
      str => ConfigValueFactory.fromAnyRef(str),
      arr => ConfigValueFactory.fromIterable(arr.map(jsonToConfigValue).asJava),
      obj => ConfigValueFactory.fromMap(obj.toMap.mapValues(jsonToConfigValue).toMap.asJava)
    )

}
