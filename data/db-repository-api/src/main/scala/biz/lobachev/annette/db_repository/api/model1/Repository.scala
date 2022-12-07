package biz.lobachev.annette.db_repository.api.model1

case class Repository(
  id: String,
  name: String,
  description: String,
  repositoryPackage: String,
  entities: Map[String, Entity],
  domains: Map[String, Domain]
)
