package biz.lobachev.annette.microservice_core.test.db

import biz.lobachev.annette.microservice_core.db.CassandraTableBuilder
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CassandraTableBuilderSpec extends AnyWordSpec with Matchers {
  import CassandraTableBuilder.types._

  "CassandraTableBuilder" should {
    "create table ddl" in {

      val ddl = CassandraTableBuilder("new_table")
        .column("id", Text, true)
        .column("name", Text)
        .column("bool", Boolean)
        .column("ts", Timestamp)
        .column("dt", Date)
        .column("tm", Time)
        .column("dur", Duration)
        .column("tmuuid", Timeuuid)
        .column("uui", Uuid)
        .column("cnt", Counter)
        .column("dbl", Double)
        .column("fl", Float)
        .column("dcm", Decimal)
        .column("in", Int)
        .column("custom", Custom("bigint"))
        .column("st", Set(Text))
        .column("lst", List(Int))
        .column("mp", Map(Text, Int))
        .build
      ddl shouldBe """CREATE TABLE IF NOT EXISTS new_table (
                     |  id text PRIMARY KEY,
                     |  name text,
                     |  bool boolean,
                     |  ts timestamp,
                     |  dt date,
                     |  tm time,
                     |  dur duration,
                     |  tmuuid timeuuid,
                     |  uui uuid,
                     |  cnt counter,
                     |  dbl double,
                     |  fl float,
                     |  dcm decimal,
                     |  in int,
                     |  custom bigint,
                     |  st set<text>,
                     |  lst list<int>,
                     |  mp map<text, int>
                     |)""".stripMargin
    }

    "create primary key" in {
      val ddl = CassandraTableBuilder("new_table")
        .column("key1", Text)
        .column("key2", Text)
        .column("key3", Text)
        .column("name", Text)
        .withPrimaryKey("key1", "key2", "key3")
        .build
      ddl shouldBe """CREATE TABLE IF NOT EXISTS new_table (
                     |  key1 text,
                     |  key2 text,
                     |  key3 text,
                     |  name text,
                     |  primary key ( key1, key2, key3 )
                     |)""".stripMargin
    }

    "create partition & clustering keys" in {
      val ddl = CassandraTableBuilder("new_table")
        .column("key1", Text)
        .column("key2", Text)
        .column("key3", Text)
        .column("name", Text)
        .withPartitionKey("key1", "key2")
        .withClusteringKey("key3")
        .build
      ddl shouldBe """CREATE TABLE IF NOT EXISTS new_table (
                     |  key1 text,
                     |  key2 text,
                     |  key3 text,
                     |  name text,
                     |  primary key ( (key1, key2), key3 )
                     |)""".stripMargin
    }
  }

}
