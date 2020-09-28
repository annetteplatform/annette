package biz.lobachev.annette.attributes.api.attribute.test

import java.time.{LocalDate, LocalTime, OffsetDateTime}

import biz.lobachev.annette.attributes.api.assignment._
import org.scalacheck.Arbitrary
import org.scalatest.enablers.TableAsserting
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.propspec.AnyPropSpec
import play.api.libs.json.Json

class AttributeJsonPropSpec extends AnyPropSpec with TableDrivenPropertyChecks with Matchers {

  import ImplicitJavaTimeGenerators._
  import org.scalacheck.Gen

  implicit val parameters: Gen.Parameters = Gen.Parameters.default

  def forAllValues[A, ASSERTION](fun: scala.Function1[A, ASSERTION])(
    implicit
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
      val attr: Attribute  = StringAttribute(value)
      val json             = Json.toJson(attr).toString
      val attr2: Attribute = Json.parse(json).as[StringAttribute]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize boolean") {
    forAllValues { value: Boolean =>
      //println(value)
      val attr: Attribute  = BooleanAttribute(value)
      val json             = Json.toJson(attr).toString
      val attr2: Attribute = Json.parse(json).as[BooleanAttribute]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize long") {
    forAllValues { value: Long =>
      val attr: Attribute  = LongAttribute(value)
      val json             = Json.toJson(attr).toString
      val attr2: Attribute = Json.parse(json).as[LongAttribute]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize double") {
    forAllValues { value: Double =>
      val attr: Attribute  = DoubleAttribute(value)
      val json             = Json.toJson(attr).toString
      val attr2: Attribute = Json.parse(json).as[DoubleAttribute]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize OffsetDateTime") {
    forAllValues { value: OffsetDateTime =>
      val attr: Attribute  = OffsetDateTimeAttribute(value)
      val json             = Json.toJson(attr).toString
      val attr2: Attribute = Json.parse(json).as[OffsetDateTimeAttribute]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize LocalDate") {
    forAllValues { value: LocalDate =>
      val attr: Attribute  = LocalDateAttribute(value)
      val json             = Json.toJson(attr).toString
      val attr2: Attribute = Json.parse(json).as[LocalDateAttribute]
      attr2 shouldBe attr
    }
  }

  property("serialize/deserialize LocalTime") {
    forAllValues { value: LocalTime =>
      val attr: Attribute  = LocalTimeAttribute(value)
      val json             = Json.toJson(attr).toString
      val attr2: Attribute = Json.parse(json).as[LocalTimeAttribute]
      attr2 shouldBe attr
    }
  }

}
