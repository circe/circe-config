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
        /*
         * FIXME: Ideally this should be:
         *
         *    parse(print(json)) <-> Right(json)
         *
         * However, loss of precision prohibits this.
         */
        parse(print(json)) <-> parse(print(parse(print(json)).right.get))
      }
    )
  }

  implicit val arbitraryConfigJson: Arbitrary[Json] = Arbitrary {
    def normalize(json: Json): Json = json
      .mapObject(_.filterKeys(_.nonEmpty).withJsons(normalize))
      .mapArray(_.map(normalize))
      .mapNumber(_ match {
        case number if number.toDouble.isInfinite => Json.fromDouble(42).get.asNumber.get
        case number => number
      })

    for (jsonObject <- testing.instances.arbitraryJsonObject.arbitrary)
      yield normalize(Json.fromJsonObject(jsonObject))
  }

  checkAll("typesafe-config", printerRoundTrip(printer.print(_), parser.parse))
}
