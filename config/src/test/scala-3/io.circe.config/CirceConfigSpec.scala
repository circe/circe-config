/*
 * Copyright 2017 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2017 Jonas Fonseca
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Based on https://github.com/jonas/circe-config/blob/0.2.1/src/test/scala/io.github.jonas.circe.config/CirceConfigSpec.scala
 */

package io.circe.config

import cats.effect.IO
import com.typesafe.config.{parser as _, *}
import io.circe.config.syntax.*
import io.circe.{parser as _, *}
import munit.CatsEffectSuite

import java.time.Period
import scala.concurrent.duration.*
import scala.io.Source

class CirceConfigSpec extends CatsEffectSuite {
  import CirceConfigSpec._

  def testParser(parse: Either[ParsingFailure, Json], decode: Either[Error, TestConfig]): Unit = {

    assert(parse.isRight)

    val Right(config) = decode

    assertEquals(config, DecodedTestConfig)
    assertEqualsDouble(config.k.getDouble("ka"), 1.1, 1e-6)
    assertEquals(config.k.getString("kb"), "abc")
    assertEquals(config.l.unwrapped, "localhost")
  }

  test("parser should parse and decode config from string") {
    testParser(
      parser.parse(AppConfigString),
      parser.decode[TestConfig](AppConfigString)
    )
  }

  test("parser should parse and decode config from object") {
    testParser(
      parser.parse(AppConfig),
      parser.decode[TestConfig](AppConfig)
    )
  }

  test("parser should parse and decode config from file") {
    def file = resolveFile("CirceConfigSpec.conf")
    testParser(
      parser.parseFile(file),
      parser.decodeFile[TestConfig](file)
    )
  }

  test("parser should parse and decode config from default typesafe config resolution") {
    assertEquals(parser.decode[AppSettings](), Right(DecodedAppSettings))
  }

  test("parser should parse and decode config from default typesafe config resolution via ApplicativeError") {
    assertIO(parser.decodeF[IO, AppSettings](), DecodedAppSettings)
  }

  test("parser should parse and decode config from default typesafe config resolution with path via ApplicativeError") {
    assertIO(parser.decodePathF[IO, HttpSettings]("http"), DecodedAppSettings.http)
  }

  test("printer should print it into a config string") {
    val Right(json) = parser.parse(AppConfig)
    val printed = io.circe.config.printer.print(json)
    val expected = readFile("CirceConfigSpec.printed.conf")
    assertEquals(printed, expected)
  }

  test("syntax should provide Config decoder") {
    assertEquals(AppConfig.as[TestConfig], Right(DecodedTestConfig))
  }

  test("syntax should provide syntax to decode at a given path") {
    assertEquals(AppConfig.as[Nested]("e"), Right(Nested(true)))
  }

  test("syntax should provide Config decoder via ApplicativeError") {
    assertIO(AppConfig.asF[IO, TestConfig], DecodedTestConfig)
  }

  test("syntax should provide syntax to decode at a given path via ApplicativeError") {
    assertIO(AppConfig.asF[IO, Nested]("e"), Nested(true))
  }

  test("round-trip should parse and print") {
    for (file <- testResourcesDir.listFiles) {
      val Right(json) = parser.parseFile(file)
      val printed = io.circe.config.printer.print(json)
      assertEquals(parser.parse(printed), Right(json), s"round-trip failed for ${file.getName}")
    }
  }
}

object CirceConfigSpec {
  val testResourcesDir = new java.io.File("config/src/test/resources")
  def resolveFile(name: String) = new java.io.File(testResourcesDir, name)
  def readFile(path: String) = Source.fromFile(resolveFile(path)).getLines().mkString("\n")

  val AppConfig: Config = ConfigFactory.parseResources("CirceConfigSpec.conf")
  val AppConfigString: String = readFile("CirceConfigSpec.conf")

  sealed abstract class Adder[T] {
    def add(a: T, b: T): T
  }
  implicit def numericAdder[T: scala.math.Numeric]: Adder[T] = new Adder[T] {
    override def add(a: T, b: T): T = implicitly[scala.math.Numeric[T]].plus(a, b)
  }

  case class TypeWithAdder[T: Adder](typeWithAdder: T)
  case class Nested(obj: Boolean) derives Decoder
  case class TestConfig(
    a: Int,
    b: Boolean,
    c: String,
    d: Option[String],
    e: Nested,
    f: List[Double],
    g: List[List[String]],
    h: List[Nested],
    i: FiniteDuration,
    j: ConfigMemorySize,
    k: Config,
    l: ConfigValue,
    m: TypeWithAdder[Int],
    n: Double,
    o: Double,
    p: Period
  ) derives Decoder

  case class ServerSettings(
    host: String,
    port: Int,
    timeout: FiniteDuration,
    maxUpload: ConfigMemorySize
  ) derives Decoder
  case class HttpSettings(
    version: Double,
    server: ServerSettings
  ) derives Decoder
  case class AppSettings(http: HttpSettings) derives Decoder

  val DecodedAppSettings = AppSettings(
    HttpSettings(
      1.1,
      ServerSettings(
        "localhost",
        8080,
        5.seconds,
        ConfigMemorySize.ofBytes(5242880)
      )
    )
  )

  val DecodedTestConfig = TestConfig(
    a = 42,
    b = false,
    c = "http://example.org",
    d = None,
    e = Nested(obj = true),
    f = List(0, .2, 123.4),
    g = List(List("nested", "list")),
    h = List(Nested(obj = true), Nested(obj = false)),
    i = 7357.seconds,
    j = ConfigMemorySize.ofBytes(134217728),
    k = ConfigFactory.parseString("ka = 1.1, kb = abc"),
    l = ConfigValueFactory.fromAnyRef("localhost"),
    m = TypeWithAdder(12),
    n = 0.0,
    o = 0,
    p = Period.ofWeeks(4)
  )

  implicit def typeWithAdderDecoder[T: Adder](implicit adderDecoder: Decoder[T]): Decoder[TypeWithAdder[T]] = {
    hCursor =>
      for {
        typeWithAdder <- hCursor.downField("typeWithAdder").as[T]
      } yield TypeWithAdder(typeWithAdder)
  }
}
