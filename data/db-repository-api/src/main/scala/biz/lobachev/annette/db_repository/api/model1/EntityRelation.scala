package biz.lobachev.annette.db_repository.api.model1

case class EntityRelation (
  id: String,
  name: String,
  description: String,
  refEntity: String,
  fields: Map[String, EntityRelationField]
                          )
