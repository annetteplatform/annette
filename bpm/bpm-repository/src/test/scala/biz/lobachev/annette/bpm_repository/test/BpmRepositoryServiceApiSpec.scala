package biz.lobachev.annette.bpm_repository.test

import biz.lobachev.annette.bpm_repository.api.domain.{Code, Notation}
import biz.lobachev.annette.bpm_repository.api.model.InvalidModel
import biz.lobachev.annette.bpm_repository.impl.BpmRepositoryServiceApiImpl
import biz.lobachev.annette.core.exception.AnnetteTransportException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class BpmRepositoryServiceApiSpec extends AnyWordSpecLike with Matchers {

  val service = new BpmRepositoryServiceApiImpl()

  "BpmRepositoryServiceApi" should {

    "extract code from BPMN" in {
      val notation = Notation.BPMN
      val xml      = BpmRepositoryServiceApiSpecData.bpmnXml
      service.extractCode(notation, xml) shouldBe Code("ApproveExpenses")
    }

    "extract code from DMN" in {
      val notation = Notation.DMN
      val xml      = BpmRepositoryServiceApiSpecData.dmnXml
      service.extractCode(notation, xml) shouldBe Code("Decision_0ek7xn0")
    }

    "extract code from CMMN" in {
      val notation = Notation.CMMN
      val xml      = BpmRepositoryServiceApiSpecData.cmmnXml
      service.extractCode(notation, xml) shouldBe Code("loanApplication")
    }

    "extract code from invalid XML" in {
      val notation = Notation.CMMN
      val xml      = BpmRepositoryServiceApiSpecData.invalidXml
      val ex       = the[AnnetteTransportException] thrownBy service.extractCode(notation, xml)
      ex shouldBe InvalidModel(notation, xml)
    }
  }
}
