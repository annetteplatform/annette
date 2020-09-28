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
      val exception =
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
      deserializedException shouldBe a[TransportException]
    }

    "serialize/deserialize RuntimeException" in {
      val exception             = new RuntimeException("Some exception")
      val serializedMessage     = serializer.serialize(exception, Seq())
      val deserializedException = serializer.deserialize(serializedMessage)
      println(s"deserialized exception:  $deserializedException")
      deserializedException shouldBe a[TransportException]
      deserializedException.asInstanceOf[TransportException].errorCode shouldBe TransportErrorCode.InternalServerError
    }
  }

}
