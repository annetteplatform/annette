package biz.lobachev.annette.core.test.exception

import biz.lobachev.annette.core.exception.{AnnetteTransportException, AnnetteTransportExceptionSerializer}
import com.lightbend.lagom.scaladsl.api.transport.{TransportErrorCode, TransportException}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.{Environment, Mode}

class SerializerSpec extends AnyWordSpec with Matchers {

  val serializer = new AnnetteTransportExceptionSerializer(Environment.simple(mode = Mode.Prod))

  "AnnetteTransportExceptionSerializer" should {
    "serialize/deserialize AnnetteTransportException" in {
      val exception             =
        AnnetteTransportException(TransportErrorCode.BadRequest, "abc", Map("param1" -> "value1", "param2" -> "value2"))
      val serializedMessage     = serializer.serialize(exception, Seq())
      val deserializedException = serializer.deserialize(serializedMessage)
      println(s"deserialized exception:  $deserializedException")
      deserializedException shouldBe a[AnnetteTransportException]
      deserializedException shouldBe exception
    }

    "serialize/deserialize TransportException" in {
      val exception             = new TransportException(TransportErrorCode.BadRequest, new RuntimeException("Some exception"))
      val serializedMessage     = serializer.serialize(exception, Seq())
      val deserializedException = serializer.deserialize(serializedMessage)
      println(s"deserialized exception:  $deserializedException")
      deserializedException shouldBe a[AnnetteTransportException]
      val ate                   = deserializedException.asInstanceOf[AnnetteTransportException]
      ate.errorCode shouldBe exception.errorCode
      ate.params("code0") shouldBe exception.getCause.getClass.getCanonicalName
      ate.params("message0") shouldBe exception.getCause.getMessage
    }

    "serialize/deserialize RuntimeException" in {
      val exception             = new RuntimeException("Some exception")
      val serializedMessage     = serializer.serialize(exception, Seq())
      val deserializedException = serializer.deserialize(serializedMessage)
      println(s"deserialized exception:  $deserializedException")
      val ate                   = deserializedException.asInstanceOf[AnnetteTransportException]
      ate.errorCode shouldBe TransportErrorCode.InternalServerError
      ate.params("code0") shouldBe exception.getClass.getCanonicalName
      ate.params("message0") shouldBe exception.getMessage
    }

    "serialize/deserialize multilevel throwable" in {
      val exception             = new RuntimeException(
        "level0",
        new IllegalArgumentException(
          "level1",
          new NoSuchElementException("level2")
        )
      )
      val serializedMessage     = serializer.serialize(exception, Seq())
      val deserializedException = serializer.deserialize(serializedMessage)
      println(s"deserialized exception:  $deserializedException")
      val ate                   = deserializedException.asInstanceOf[AnnetteTransportException]
      ate.errorCode shouldBe TransportErrorCode.InternalServerError
      ate.params("code0") shouldBe exception.getClass.getCanonicalName
      ate.params("message0") shouldBe exception.getMessage
      ate.params("code1") shouldBe exception.getCause.getClass.getCanonicalName
      ate.params("message1") shouldBe exception.getCause.getMessage
      ate.params("code2") shouldBe exception.getCause.getCause.getClass.getCanonicalName
      ate.params("message2") shouldBe exception.getCause.getCause.getMessage
    }
  }

}
