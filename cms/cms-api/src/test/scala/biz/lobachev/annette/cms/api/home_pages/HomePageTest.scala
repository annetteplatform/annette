package biz.lobachev.annette.cms.api.home_pages

import biz.lobachev.annette.core.model.auth.PersonPrincipal
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HomePageTest extends AnyWordSpec with Matchers {

  "HomePageTest" should {
    "toCompositeId" in {
      HomePage.toCompositeId("app-id", PersonPrincipal("P0001")) shouldBe "app-id~person~P0001"
    }

    "fromCompositeId" in {
      HomePage.fromCompositeId("app-id~person~P0001") shouldBe "app-id" -> PersonPrincipal("P0001")
    }
  }
}
