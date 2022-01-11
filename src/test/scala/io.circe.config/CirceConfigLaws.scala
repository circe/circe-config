package io.circe.config

import cats.instances.either._
import cats.laws._
import cats.laws.discipline._
import com.typesafe.config.{parser => _, _}
import io.circe.testing.ParserTests
import io.circe.{Decoder, Json, Parser, ParsingFailure}
import munit.ScalaCheckSuite
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

class CirceConfigLaws extends ScalaCheckSuite {

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

  def checkLaws(name: String, ruleSet: Laws#RuleSet): Unit =
    ruleSet.all.properties.foreach { case (id, prop) =>
      property(s"$name should obey $id")(prop)
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
