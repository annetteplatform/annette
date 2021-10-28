package biz.lobachev.annette.core.test.exception

import biz.lobachev.annette.core.exception.{
  AnnetteTransportExceptionCompanion,
  AnnetteTransportExceptionCompanion1,
  AnnetteTransportExceptionCompanion2,
  AnnetteTransportExceptionCompanion3
}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

object Exception0 extends AnnetteTransportExceptionCompanion {
  override val ErrorCode: TransportErrorCode = TransportErrorCode.BadRequest
  override val MessageCode: String           = "exception"
}

object Exception1 extends AnnetteTransportExceptionCompanion1 {
  override val ErrorCode: TransportErrorCode = TransportErrorCode.BadRequest
  override val MessageCode: String           = "exception"
  override val Arg1Key: String               = "key1"
}

object Exception2 extends AnnetteTransportExceptionCompanion2 {
  override val ErrorCode: TransportErrorCode = TransportErrorCode.BadRequest
  override val MessageCode: String           = "exception"
  override val Arg1Key: String               = "key1"
  override val Arg2Key: String               = "key2"
}

object Exception3 extends AnnetteTransportExceptionCompanion3 {
  override val ErrorCode: TransportErrorCode = TransportErrorCode.BadRequest
  override val MessageCode: String           = "exception"
  override val Arg1Key: String               = "key1"
  override val Arg2Key: String               = "key2"
  override val Arg3Key: String               = "key3"
}

class ExceptionCompanionSpec extends AnyWordSpec with Matchers {

  "Exception companion" should {

    "0 params" in {
      val exception = Exception0()
      exception match {
        case Exception0(_) => assert(true)
        case _             => assert(false)
      }
    }

    "1 params" in {
      val exception = Exception1("param1")
      exception match {
        case Exception1(arg1) => arg1 shouldBe exception.params(Exception1.Arg1Key)
        case _                => assert(false)
      }
    }

    "2 params" in {
      val exception = Exception2("param1", "param2")
      exception match {
        case Exception2(arg1, arg2) =>
          arg1 shouldBe exception.params(Exception2.Arg1Key)
          arg2 shouldBe exception.params(Exception2.Arg2Key)
        case _                      => assert(false)
      }
    }

    "3 params" in {
      val exception = Exception3("param1", "param2", "param2")
      exception match {
        case Exception3(arg1, arg2, arg3) =>
          arg1 shouldBe exception.params(Exception3.Arg1Key)
          arg2 shouldBe exception.params(Exception3.Arg2Key)
          arg3 shouldBe exception.params(Exception3.Arg3Key)
        case _                            => assert(false)
      }
    }

  }

}
