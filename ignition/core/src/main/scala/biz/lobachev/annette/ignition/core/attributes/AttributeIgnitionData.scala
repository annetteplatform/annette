package biz.lobachev.annette.ignition.core.attributes

case class AttributeIgnitionData(
  schemas: Seq[SchemaIgnitionData] = Seq.empty
)

case class SchemaIgnitionData(
  id: String,
  sub: Option[String],
  name: String,
  schemaFile: Option[String],
  attrFiles: Seq[String]
)
