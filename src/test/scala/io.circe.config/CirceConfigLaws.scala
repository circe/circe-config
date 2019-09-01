package io.circe.config

import cats.instances.either._
import cats.laws._
import cats.laws.discipline._
import io.circe.{ Decoder, Json, Parser, ParsingFailure }
import io.circe.testing.ParserTests
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.Checkers
import org.scalacheck.{ Arbitrary, Prop }
import org.typelevel.discipline.Laws
import com.typesafe.config.{parser => _, _}

class CirceConfigLaws extends AnyFlatSpec {

  implicit val arbitraryConfigJson: Arbitrary[Json] = Arbitrary {
    def normalize(json: Json): Json = json
      .mapObject(_.filterKeys(_.nonEmpty).mapValues(normalize))
      .mapArray(_.map(normalize))
      .mapNumber(number => {
        def numberToDouble = {
          val double = number.toDouble
          if (double.isWhole)
            Some(Json.fromLong(double.toLong))
          else
            Json.fromDouble(double)
        }
        // Map to the three principal types supported by Typesafe Config: Int, Long or Double
        val json =
          number.toInt.map(Json.fromInt) orElse
            number.toLong.map(Json.fromLong) orElse
            numberToDouble getOrElse
            Json.fromInt(42)

        json.asNumber.get
      })

    for (jsonObject <- io.circe.testing.instances.arbitraryJsonObject.arbitrary)
      yield normalize(Json.fromJsonObject(jsonObject))
  }

  def checkLaws(name: String, ruleSet: Laws#RuleSet): Unit = ruleSet.all.properties.zipWithIndex.foreach {
    case ((id, prop), 0) => name should s"obey $id" in Checkers.check(prop)
    case ((id, prop), _) => it should s"obey $id" in Checkers.check(prop)
  }

  checkLaws("Parser", ParserTests(parser).fromString)
  checkLaws("Parser", ParserTests(parser).fromFunction[Config]("fromConfig")(
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
