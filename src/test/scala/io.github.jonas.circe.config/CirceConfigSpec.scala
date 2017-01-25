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
import io.circe._, io.circe.generic.semiauto._

object CirceConfigSpec {
  case class Nested(obj: Boolean)
  object Nested {
    implicit val decoder: Decoder[Nested] = deriveDecoder
  }

  case class TestConfig(
    a: Int,
    b: Boolean,
    c: String,
    d: Option[String],
    e: Nested,
    f: List[Double],
    g: List[List[String]],
    h: List[Nested]
  )
  object TestConfig {
    implicit val decoder: Decoder[TestConfig] = deriveDecoder
  }
}

class CirceConfigSpec extends FlatSpec with Matchers {
  import CirceConfigSpec._

  val ConfigString = """
    a = 42
    b = false
    c = "http://example.org"
    d = null
    e = { obj = true }
    f = [ 0, 0.2, 123.4 ]
    g = [ [ nested, list ] ]
    h = [ { obj = true }, { obj: false } ]
  """

  val LoadedConfig = TestConfig(
    a = 42,
    b = false,
    c = "http://example.org",
    d = None,
    e = Nested(obj = true),
    f = List(0, .2, 123.4),
    g = List(List("nested", "list")),
    h = List(Nested(obj = true), Nested(obj = false))
  )

  "ConfigParser" should "read simple config file" in {
    assert(parser.parse(ConfigString).isRight)
  }

  it should "decode it into a case class" in {
    assert(parser.decode[TestConfig](ConfigString) == Right(LoadedConfig))
  }

  it should "print it into a config string" in {
    val json = parser.parse(ConfigString)
    val opts = ConfigRenderOptions.defaults.setJson(false)
    val printedConfigString = printer.print(json.right.get, opts)
    assert(parser.parse(printedConfigString) == json)
  }

}
