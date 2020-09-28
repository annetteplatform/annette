package biz.lobachev.annette.attributes.api.attribute.test

class AttributeAssignmentPropSpec {}

import biz.lobachev.annette.attributes.api.assignment.AttributeAssignmentId
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

object AttributeAssignmentPropSpec extends Properties("String") {

  property("decode/encode attribute assignment id") = forAll {
    (schemaId: String, subSchemaId: Option[String], attributeId: String, objectId: String) =>
      val id       = AttributeAssignmentId(
        schemaId,
        subSchemaId,
        objectId,
        attributeId
      )
      val composed = id.toComposed
      val resId    = AttributeAssignmentId.fromComposed(composed)
//    println(id)
//    println(composed)
//    println(resId)
//    println
//    println
      id == resId
  }

}
