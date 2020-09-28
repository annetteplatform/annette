package biz.lobachev.annette.attributes.api.attribute.test

import java.time._
import java.time.chrono._

import org.scalacheck.Gen._
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.TableFor1

import scala.jdk.CollectionConverters._

object ImplicitJavaTimeGenerators extends ImplicitJavaTimeGenerators
trait ImplicitJavaTimeGenerators {

  implicit val arbChronology: Arbitrary[Chronology] =
    Arbitrary(
      oneOf(
        Seq(
          HijrahChronology.INSTANCE,
          IsoChronology.INSTANCE,
          JapaneseChronology.INSTANCE,
          MinguoChronology.INSTANCE,
          ThaiBuddhistChronology.INSTANCE
        )
      )
    )

  implicit val arbZoneId: Arbitrary[ZoneId] =
    Arbitrary {
      Gen.oneOf(ZoneId.getAvailableZoneIds.asScala.toSeq.map(ZoneId.of))
    }

  implicit val arbInstant: Arbitrary[Instant] =
    Arbitrary {
      for {
        millis <- chooseNum(
                    OffsetDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant.getEpochSecond,
                    OffsetDateTime.of(2099, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant.getEpochSecond
                  )
        nanos  <- chooseNum(Instant.MIN.getNano, Instant.MAX.getNano)
      } yield Instant.ofEpochSecond(millis).plusNanos(nanos.toLong)
    }

  implicit val arbLocalDate: Arbitrary[LocalDate] =
    Arbitrary {
      for {
        epochDay <- chooseNum(LocalDate.MIN.toEpochDay, LocalDate.MAX.toEpochDay)
      } yield LocalDate.ofEpochDay(epochDay)
    }

  implicit val arbLocalTime: Arbitrary[LocalTime] =
    Arbitrary {
      for {
        nanoOfDay <- chooseNum(LocalTime.MIN.toNanoOfDay, LocalTime.MAX.toNanoOfDay)
      } yield LocalTime.ofNanoOfDay(nanoOfDay)
    }

  implicit lazy val arbLocalDateTime: Arbitrary[LocalDateTime] = {
    import ZoneOffset.UTC
    Arbitrary {
      for {
        seconds <- chooseNum(LocalDateTime.MIN.toEpochSecond(UTC), LocalDateTime.MAX.toEpochSecond(UTC))
        nanos   <- chooseNum(LocalDateTime.MIN.getNano, LocalDateTime.MAX.getNano)
      } yield LocalDateTime.ofEpochSecond(seconds, nanos, UTC)
    }
  }

  implicit lazy val arbZonedDateTime: Arbitrary[ZonedDateTime] =
    Arbitrary {
      for {
        zoneId  <- arbZoneId.arbitrary
        instant <- arbInstant.arbitrary
      } yield ZonedDateTime.ofInstant(instant, zoneId)
    }

  implicit lazy val arbOffsetDateTime: Arbitrary[OffsetDateTime] =
    Arbitrary {
      for {
        zoned <- arbZonedDateTime.arbitrary
      } yield zoned.toOffsetDateTime
    }

  implicit class ArbitraryToTable[A](arbitrary: Arbitrary[A]) {
    def toTable(heading: String)(implicit parameters: Gen.Parameters): TableFor1[A] = {
      val list: Seq[A] = Gen
        .listOfN(parameters.size, arbitrary.arbitrary)(parameters, parameters.initialSeed.getOrElse(Seed.random()))
        .get
      new TableFor1(heading, list: _*)
    }
  }

}
