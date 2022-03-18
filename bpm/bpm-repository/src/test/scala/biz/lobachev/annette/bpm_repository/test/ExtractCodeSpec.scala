package biz.lobachev.annette.bpm_repository.test

import biz.lobachev.annette.bpm_repository.api.domain.Notation
import biz.lobachev.annette.bpm_repository.api.model.InvalidModel
import biz.lobachev.annette.bpm_repository.impl.model.CodeExtractor
import biz.lobachev.annette.core.exception.AnnetteTransportException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ExtractCodeSpec extends AnyWordSpecLike with Matchers {

  val bpmModelService = new CodeExtractor() {}

  "BpmModelService" should {

    "extract code from BPMN" in {
      val notation = Notation.BPMN
      val xml      = BpmRepositoryData.bpmnXml
      bpmModelService.extractCode(notation, xml) shouldBe "ApproveExpenses"
    }

    "extract code from DMN" in {
      val notation = Notation.DMN
      val xml      = BpmRepositoryData.dmnXml
      bpmModelService.extractCode(notation, xml) shouldBe "Decision_0ek7xn0"
    }

    "extract code from CMMN" in {
      val notation = Notation.CMMN
      val xml      = BpmRepositoryData.cmmnXml
      bpmModelService.extractCode(notation, xml) shouldBe "loanApplication"
    }

    "extract code from invalid XML" in {
      val notation = Notation.CMMN
      val xml      = BpmRepositoryData.invalidXml
      val ex       = the[AnnetteTransportException] thrownBy bpmModelService.extractCode(notation, xml)
      ex shouldBe InvalidModel(notation, xml)
    }
  }
}
