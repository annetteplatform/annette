package biz.lobachev.annette.db_repository.api.model2

case class EntityIndex(
  id: String,
  name: String,
  description: String,
  entity: String,
  unique: Boolean,
  fields: Seq[String]
)
