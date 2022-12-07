package biz.lobachev.annette.db_repository.api.model2

case class Repository(
  id: String,
  name: String,
  description: String,
  repositoryPackage: String,
  entities: Map[String, Entity],
  fields: Map[String, EntityField],
  indexes: Map[String, EntityIndex],
  relations: Map[String, EntityRelation],
  domains: Map[String, Domain]
)
