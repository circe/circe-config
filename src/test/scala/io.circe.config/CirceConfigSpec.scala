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
package io.circe.config

import cats.effect.IO
import munit.FunSuite
import com.typesafe.config.{parser => _, _}
import io.circe.{parser => _, _}
import io.circe.generic.auto._

import scala.concurrent.duration._
import java.time.Period
import scala.io.Source
import io.circe.config.syntax._

class CirceConfigSpec extends FunSuite {

  import CirceConfigSpec._

  trait ParserTests {
    def parse: Either[ParsingFailure, Json]

    def decode: Either[Error, TestConfig]

    assert(parse.isRight)

    val Right(config) = decode

    assert(config == DecodedTestConfig)
    assert(config.k.getDouble("ka") == 1.1)
    assert(config.k.getString("kb") == "abc")
    assert(config.l.unwrapped == "localhost")
  }

  def parserAssertions(parse: Either[ParsingFailure, Json], decode: Either[Error, TestConfig]): Unit = {
    assert(parse.isRight)

    val Right(config) = decode

    assert(config == DecodedTestConfig)
    assert(config.k.getDouble("ka") == 1.1)
    assert(config.k.getString("kb") == "abc")
    assert(config.l.unwrapped == "localhost")
  }

  test("parser should parse and decode config from string") {
    parserAssertions(
      parser.parse(AppConfigString),
      parser.decode[TestConfig](AppConfigString)
    )
  }

  test("parser should parse and decode config from object") {
    parserAssertions(
      parser.parse(AppConfig),
      parser.decode[TestConfig](AppConfig)
    )
  }

  test("parser should parse and decode config from file") {
    def file = resolveFile("CirceConfigSpec.conf")

    parserAssertions(parser.parseFile(file), parser.decodeFile[TestConfig](file))
  }

  test("parser should parse and decode config from default typesafe config resolution") {
    parser
      .decode[AppSettings]()
      .fold(
        fail("failed to decode typesafe config", _),
        s => assert(s == DecodedAppSettings)
      )
  }

  test("parser should parse and decode config from default typesafe config resolution via ApplicativeError") {
    assert(parser.decodeF[IO, AppSettings]().unsafeRunSync() == DecodedAppSettings)
  }

  test("parser should parse and decode config from default typesafe config resolution with path via ApplicativeError") {
    assert(parser.decodePathF[IO, HttpSettings]("http").unsafeRunSync() == (DecodedAppSettings.http))
  }

  test("printer should print it into a config string") {
    val Right(json) = parser.parse(AppConfig)
    val expected = readFile("CirceConfigSpec.printed.conf")
    assert(printer.print(json) == expected)
  }

  test("syntax should provide Config decoder") {
    assert(AppConfig.as[TestConfig] == Right(DecodedTestConfig))
  }

  test("provide syntax to decode at a given path") {
    assert(AppConfig.as[Nested]("e") == Right(Nested(true)))
  }

  test("provide Config decoder via ApplicativeError") {
    assert(AppConfig.asF[IO, TestConfig].unsafeRunSync() == DecodedTestConfig)
  }

  test("provide syntax to decode at a given path via ApplicativeError") {
    assert(AppConfig.asF[IO, Nested]("e").unsafeRunSync() == Nested(true))
  }

  test("round-trip should parse and print") {
    for (file <- testResourcesDir.listFiles) {
      val Right(json) = parser.parseFile(file)
      assert(parser.parse(printer.print(json)) == Right(json), s"round-trip failed for ${file.getName}")
    }
  }
}

object CirceConfigSpec {
  val testResourcesDir = new java.io.File("src/test/resources")
  def resolveFile(name: String) = new java.io.File(testResourcesDir, name)
  def readFile(path: String) = Source.fromFile(resolveFile(path)).getLines.mkString("\n")

  val AppConfig: Config = ConfigFactory.parseResources("CirceConfigSpec.conf")
  val AppConfigString: String = readFile("CirceConfigSpec.conf")

  sealed abstract class Adder[T] {
    def add(a: T, b: T): T
  }
  implicit def numericAdder[T: scala.math.Numeric]: Adder[T] = new Adder[T] {
    override def add(a: T, b: T): T = implicitly[scala.math.Numeric[T]].plus(a, b)
  }

  case class TypeWithAdder[T: Adder](typeWithAdder: T)
  case class Nested(obj: Boolean)
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
  )

  case class ServerSettings(
    host: String,
    port: Int,
    timeout: FiniteDuration,
    maxUpload: ConfigMemorySize
  )
  case class HttpSettings(
    version: Double,
    server: ServerSettings
  )
  case class AppSettings(http: HttpSettings)

  val DecodedAppSettings = AppSettings(
    HttpSettings(
      1.1,
      ServerSettings(
        "localhost",
        8080,
        5 seconds,
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
    i = 7357 seconds,
    j = ConfigMemorySize.ofBytes(134217728),
    k = ConfigFactory.parseString("ka = 1.1, kb = abc"),
    l = ConfigValueFactory.fromAnyRef("localhost"),
    m = TypeWithAdder(12),
    n = 0.0,
    o = 0,
    p = Period.ofWeeks(4)
  )
}
