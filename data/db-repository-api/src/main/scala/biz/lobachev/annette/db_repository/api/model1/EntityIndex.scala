package biz.lobachev.annette.db_repository.api.model1

case class EntityIndex(
  id: String,
  name: String,
  description: String,
  unique: Boolean,
  fields: Seq[String]
)
