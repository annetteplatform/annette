package biz.lobachev.annette.core.model.auth

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import AnnettePrincipal._

class AnnettePrincipalTest extends AnyWordSpec with Matchers {

  "AnnettePrincipalTest" should {
    "serialize/deserialize AnnettePrincipal" in {
      val originalPrincipal     = PersonPrincipal("P0001")
      println(s"originalPrincipal: $originalPrincipal")
      val json                  = Json.toJson(originalPrincipal)
      println(s"json: ${json.toString}")
      val deserializedPrincipal = Json.parse(json.toString).as[AnnettePrincipal]
      println(s"deserializedPrincipal: $deserializedPrincipal")
      deserializedPrincipal shouldBe originalPrincipal
    }
  }
}
