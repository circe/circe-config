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

import org.scalatest.{ FlatSpec, Matchers }
import com.typesafe.config._
import io.circe._
import io.circe.generic.auto._
import scala.concurrent.duration._
import scala.io.Source

import syntax._

class CirceConfigSpec extends FlatSpec with Matchers {
  import CirceConfigSpec._

  trait ParserTests {
    def parse: Either[ParsingFailure, Json]
    def decode: Either[Error, TestConfig]

    assert(parse.isRight)

    val config = decode
    assert(config == Right(DecodedTestConfig))
    assert(config.right.get.k.getDouble("ka") == 1.1)
    assert(config.right.get.k.getString("kb") == "abc")
    assert(config.right.get.l.unwrapped == "localhost")
  }

  "parser" should "parse and decode config from string" in new ParserTests {
    def parse = parser.parse(AppConfigString)
    def decode = parser.decode[TestConfig](AppConfigString)
  }

  it should "parse and decode config from object" in new ParserTests {
    def parse = parser.parse(AppConfig)
    def decode = parser.decode[TestConfig](ConfigFactory.load)
  }

  it should "parse and decode config from file" in new ParserTests {
    def file = resolveFile("application.conf")
    def parse = parser.parseFile(file)
    def decode = parser.decodeFile[TestConfig](file)
  }

  "printer" should "print it into a config string" in {
    val json = parser.parse(AppConfig)
    val expected = readFile("application.printed.conf")
    assert(printer.print(json.right.get) == expected)
  }

  "syntax" should "provide Config decoder" in {
    assert(AppConfig.as[TestConfig] == Right(DecodedTestConfig))
  }

  it should "provide syntax to decode at a given path" in {
    assert(AppConfig.as[Nested]("e") == Right(Nested(true)))
  }

  "round-trip" should "parse and print" in {
    for (file <- testResourcesDir.listFiles) {
      val json = parser.parseFile(file)
      assert(json.isRight == true, s"failed to parse ${file.getName}")
      assert(
        parser.parse(printer.print(json.right.get)) == json,
        s"round-trip failed for ${file.getName}")
    }
  }
}

object CirceConfigSpec {
  val testResourcesDir = new java.io.File("src/test/resources")
  def resolveFile(name: String) = new java.io.File(testResourcesDir, name)
  def readFile(path: String) = Source.fromFile(resolveFile(path)).getLines.mkString("\n")

  val AppConfig: Config = ConfigFactory.defaultApplication
  val AppConfigString: String = readFile("application.conf")

  sealed abstract class Adder[T] {
    def add(a: T, b: T): T
  }
  implicit def numericAdder[T: scala.math.Numeric] = new Adder[T] {
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
    o: Double
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
    o = 0
  )
}
