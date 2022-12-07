package biz.lobachev.annette.db_repository.api.model2

case class EntityRelation(
  id: String,
  name: String,
  description: String,
  entity: String,
  refEntity: String,
  fieldMap: Map[String, String]
)
