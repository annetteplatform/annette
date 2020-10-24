package biz.lobachev.annette.attributes.api.attribute.test

import java.time.{LocalDate, LocalTime, OffsetDateTime}

import biz.lobachev.annette.attributes.api.assignment._
import org.scalacheck.Arbitrary
import org.scalatest.enablers.TableAsserting
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.propspec.AnyPropSpec
import play.api.libs.json.Json

class AttributeValueJsonPropSpec extends AnyPropSpec with TableDrivenPropertyChecks with Matchers {

  import ImplicitJavaTimeGenerators._
  import org.scalacheck.Gen

  implicit val parameters: Gen.Parameters = Gen.Parameters.default

  def forAllValues[A, ASSERTION](fun: scala.Function1[A, ASSERTION])(implicit
    parameters: Gen.Parameters,
    arbitrary: Arbitrary[A],
    asserting: org.scalatest.enablers.TableAsserting[ASSERTION],
    prettifier: org.scalactic.Prettifier,
    pos: org.scalactic.source.Position
  ): TableAsserting[ASSERTION]#Result = {
    val table: TableFor1[A] = arbitrary.toTable("value")
    forAll(table)(fun)(asserting, prettifier, pos)
  }

  property("serialize/deserialize string") {
    forAllValues { value: String =>
      val attr: AttributeValue  = StringAttributeValue(value)
      val json                  = Json.toJson(attr).toString
      val attr2: AttributeValue = Json.parse(json).as[StringAttributeValue]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize boolean") {
    forAllValues { value: Boolean =>
      //println(value)
      val attr: AttributeValue  = BooleanAttributeValue(value)
      val json                  = Json.toJson(attr).toString
      val attr2: AttributeValue = Json.parse(json).as[BooleanAttributeValue]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize long") {
    forAllValues { value: Long =>
      val attr: AttributeValue  = LongAttributeValue(value)
      val json                  = Json.toJson(attr).toString
      val attr2: AttributeValue = Json.parse(json).as[LongAttributeValue]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize double") {
    forAllValues { value: Double =>
      val attr: AttributeValue  = DoubleAttributeValue(value)
      val json                  = Json.toJson(attr).toString
      val attr2: AttributeValue = Json.parse(json).as[DoubleAttributeValue]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize OffsetDateTime") {
    forAllValues { value: OffsetDateTime =>
      val attr: AttributeValue  = OffsetDateTimeAttributeValue(value)
      val json                  = Json.toJson(attr).toString
      val attr2: AttributeValue = Json.parse(json).as[OffsetDateTimeAttributeValue]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize LocalDate") {
    forAllValues { value: LocalDate =>
      val attr: AttributeValue  = LocalDateAttributeValue(value)
      val json                  = Json.toJson(attr).toString
      val attr2: AttributeValue = Json.parse(json).as[LocalDateAttributeValue]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize LocalTime") {
    forAllValues { value: LocalTime =>
      val attr: AttributeValue  = LocalTimeAttributeValue(value)
      val json                  = Json.toJson(attr).toString
      val attr2: AttributeValue = Json.parse(json).as[LocalTimeAttributeValue]
      attr2 shouldBe attr
    }
  }

}
