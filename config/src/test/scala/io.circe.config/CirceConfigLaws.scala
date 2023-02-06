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
 * Based on https://github.com/jonas/circe-config/blob/0.2.1/src/test/scala/io.github.jonas.circe.config/CirceConfigLaws.scala
 */

package io.circe.config

import cats.instances.either._
import cats.laws._
import cats.laws.discipline._
import com.typesafe.config.{parser => _, _}
import io.circe.Decoder
import io.circe.Json
import io.circe.Parser
import io.circe.ParsingFailure
import io.circe.testing.ParserTests
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.Laws

class CirceConfigLaws extends AnyFlatSpec {

  implicit val arbitraryConfigJson: Arbitrary[Json] = Arbitrary {
    def normalize(json: Json): Json =
      json.mapObject(_.filterKeys(_.nonEmpty).mapValues(normalize)).mapArray(_.map(normalize)).mapNumber { number =>
        // Lower the precision to the types used internally by
        // Lightbend Config to ensure that numbers are representable.
        val double: java.lang.Double = number.toDouble
        val long: java.lang.Long = double.toLong

        val json =
          if (double.isInfinite)
            // While +/+Infinity can be represented, it cannot be round-tripped.
            Json.fromInt(42)
          else if (long == double)
            // Emulate Lightbend Config's internal cast:
            // https://github.com/lightbend/config/blob/v1.3.4/config/src/main/java/com/typesafe/config/impl/ConfigNumber.java#L96-L104
            Json.fromLong(long)
          else
            Json.fromDouble(double).get

        json.asNumber.get
      }

    for (jsonObject <- io.circe.testing.instances.arbitraryJsonObject.arbitrary)
      yield normalize(Json.fromJsonObject(jsonObject))
  }

  def checkLaws(name: String, ruleSet: Laws#RuleSet): Unit = ruleSet.all.properties.zipWithIndex.foreach {
    case ((id, prop), 0) => name should s"obey $id" in Checkers.check(prop)
    case ((id, prop), _) => it should s"obey $id" in Checkers.check(prop)
  }

  checkLaws("Parser", ParserTests(parser).fromString)
  checkLaws(
    "Parser",
    ParserTests(parser).fromFunction[Config]("fromConfig")(
      ConfigFactory.parseString,
      _.parse,
      _.decode[Json],
      _.decodeAccumulating[Json]
    )
  )
  checkLaws("Printer", PrinterTests(parser).fromJson)
  checkLaws("Codec", CodecTests[Config](syntax.configDecoder, parser.parse).fromFunction("fromConfig"))
}

case class PrinterTests(parser: Parser) extends Laws {
  def fromJson(implicit A: Arbitrary[Json]): RuleSet =
    new DefaultRuleSet(
      name = "printer",
      parent = None,
      "roundTrip" -> Prop.forAll { (json: Json) =>
        parser.parse(printer.print(json)) <-> Right(json)
      }
    )
}

case class CodecTests[A](decoder: Decoder[A], parse: A => Either[ParsingFailure, Json]) extends Laws {
  def fromFunction(name: String)(implicit arbitraryJson: Arbitrary[Json]): RuleSet =
    new DefaultRuleSet(
      name = s"codec[$name]",
      parent = None,
      "decodingRoundTrip" -> Prop.forAll { (json: Json) =>
        decoder.decodeJson(json).flatMap(parse) <-> Right(json)
      }
    )
}
