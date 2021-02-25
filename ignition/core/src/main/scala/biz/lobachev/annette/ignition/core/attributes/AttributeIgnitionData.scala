package biz.lobachev.annette.ignition.core.attributes

protected case class AttributeIgnitionData(
  schemas: Seq[SchemaIgnitionData] = Seq.empty
)

protected case class SchemaIgnitionData(
  id: String,
  sub: Option[String],
  name: String,
  schemaFile: Option[String],
  attrFiles: Seq[String]
)
