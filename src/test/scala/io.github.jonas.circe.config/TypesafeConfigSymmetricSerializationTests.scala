package io.github.jonas.circe.config

import cats.instances.either._
import cats.laws._
import cats.laws.discipline._
import io.circe.{ Json, ParsingFailure }
import io.circe.testing
import org.scalatest.FunSuite
import org.scalacheck.{ Arbitrary, Prop }
import org.typelevel.discipline.scalatest.Discipline
import org.typelevel.discipline.Laws

class TypesafeConfigPrinterLaws extends FunSuite with Discipline with Laws {

  def printerRoundTrip(
    print: Json => String,
    parse: String => Either[ParsingFailure, Json]
  ): RuleSet = {
    new DefaultRuleSet(
      name = "printer",
      parent = None,
      "roundTrip" -> Prop.forAll { (json: Json) =>
        parse(print(json)) <-> Right(json)
      }
    )
  }

  implicit val arbitraryConfigJson: Arbitrary[Json] = Arbitrary {
    def normalize(json: Json): Json = json
      .mapObject(_.filterKeys(_.nonEmpty).withJsons(normalize))
      .mapArray(_.map(normalize))
      .mapNumber(number => {
        // Map to the three principal types supported by Typesafe Config: Int, Long or Double
        val json =
          number.toInt.map(Json.fromInt) orElse
            number.toLong.map(Json.fromLong) orElse
            Json.fromDouble(number.toDouble) getOrElse
            Json.fromInt(42)

        json.asNumber.get
      })

    for (jsonObject <- testing.instances.arbitraryJsonObject.arbitrary)
      yield normalize(Json.fromJsonObject(jsonObject))
  }

  checkAll("typesafe-config", printerRoundTrip(printer.print(_), parser.parse))
}
