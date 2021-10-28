/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.microservice_core.db

import biz.lobachev.annette.microservice_core.db.CassandraTableBuilder.ColumnDefinition
import biz.lobachev.annette.microservice_core.db.CassandraTableBuilder.types.CassandraDataType

class CassandraTableBuilder(
  name: String,
  columns: Seq[ColumnDefinition] = Seq.empty,
  partitionKey: Seq[String] = Seq.empty,
  clusteringKey: Seq[String] = Seq.empty
) {

  def column(name: String, datatype: CassandraDataType, isPrimaryKey: Boolean = false): CassandraTableBuilder =
    new CassandraTableBuilder(
      name = this.name,
      columns = this.columns :+ ColumnDefinition(name, datatype, isPrimaryKey),
      partitionKey = partitionKey,
      clusteringKey = clusteringKey
    )

  def withPrimaryKey(keys: String*) =
    new CassandraTableBuilder(
      name = name,
      columns = columns,
      partitionKey = Seq(keys.head),
      clusteringKey = keys.tail
    )

  def withPartitionKey(keys: String*) =
    new CassandraTableBuilder(
      name = name,
      columns = columns,
      partitionKey = keys,
      clusteringKey = clusteringKey
    )

  def withClusteringKey(keys: String*) =
    new CassandraTableBuilder(
      name = name,
      columns = columns,
      partitionKey = partitionKey,
      clusteringKey = keys
    )

  def build: String = {
    val columnLines = columns.map(_.buildColumnLine)
    val lines       = buildPrimaryKeyLine
      .map(pkLine => columnLines :+ pkLine)
      .getOrElse(columnLines)
      .map(line => s"  $line")
      .mkString(",\n")
    s"""CREATE TABLE IF NOT EXISTS $name (
$lines
)"""
  }

  private def buildPrimaryKeyLine: Option[String] =
    if (partitionKey.nonEmpty) {
      val partKey =
        if (partitionKey.length == 1) partitionKey.head
        else partitionKey.mkString("(", ", ", ")")
      val pk      = (partKey +: clusteringKey).mkString(", ")
      Some(s"primary key ( $pk )")
    } else None

}

object CassandraTableBuilder {
  def apply(name: String) = new CassandraTableBuilder(name)

  object types {
    sealed trait CassandraDataType {
      def cType: String
    }

    sealed trait CassandraBaseDataType extends CassandraDataType

    case object Boolean extends CassandraBaseDataType {
      override def cType: String = "boolean"
    }

    case object Text extends CassandraBaseDataType {
      override def cType: String = "text"
    }

    case object Timestamp extends CassandraBaseDataType {
      override def cType: String = "timestamp"
    }

    case object Date extends CassandraBaseDataType {
      override def cType: String = "date"
    }

    case object Time extends CassandraBaseDataType {
      override def cType: String = "time"
    }

    case object Duration extends CassandraBaseDataType {
      override def cType: String = "duration"
    }

    case object Timeuuid extends CassandraBaseDataType {
      override def cType: String = "timeuuid"
    }

    case object Uuid extends CassandraBaseDataType {
      override def cType: String = "uuid"
    }

    case object Counter extends CassandraBaseDataType {
      override def cType: String = "counter"
    }

    case object Double extends CassandraBaseDataType {
      override def cType: String = "double"
    }

    case object Float extends CassandraBaseDataType {
      override def cType: String = "float"
    }

    case object Decimal extends CassandraBaseDataType {
      override def cType: String = "decimal"
    }

    case object Int extends CassandraBaseDataType {
      override def cType: String = "int"
    }

    case class Custom(customType: String) extends CassandraBaseDataType {
      override def cType: String = customType
    }

    sealed trait CassandraCollectionDataType extends CassandraDataType

    case class Set(subtype: CassandraBaseDataType) extends CassandraCollectionDataType {
      override def cType: String = s"set<${subtype.cType}>"
    }

    case class List(subtype: CassandraBaseDataType) extends CassandraCollectionDataType {
      override def cType: String = s"list<${subtype.cType}>"
    }

    case class Map(keyType: CassandraBaseDataType, valueType: CassandraBaseDataType)
        extends CassandraCollectionDataType {
      override def cType: String = s"map<${keyType.cType}, ${valueType.cType}>"
    }

  }

  case class ColumnDefinition(name: String, datatype: CassandraDataType, isPrimaryKey: Boolean = false) {
    val pk                      = if (isPrimaryKey) " PRIMARY KEY" else ""
    def buildColumnLine: String = s"$name ${datatype.cType}$pk"
  }

}
