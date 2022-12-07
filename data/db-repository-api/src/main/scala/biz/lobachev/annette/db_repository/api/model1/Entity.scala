package biz.lobachev.annette.db_repository.api.model1

case class Entity(
  id: String,
  name: String,
  description: String,
  className: String,
  tableName: String,
  pk: Seq[String],
  fields: Map[String, EntityField],
  indexes: Map[String, EntityIndex],
  relations: Map[String, EntityRelation]
)
