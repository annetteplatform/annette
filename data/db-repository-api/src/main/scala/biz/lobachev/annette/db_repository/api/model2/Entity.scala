package biz.lobachev.annette.db_repository.api.model2

case class Entity (
                    id: String,
                    name: String,
                    description: String,
                    className: String,
                    tableName: String,
                    pk: Seq[String],
                    fields: Seq[String],
                    indexes: Set[String],
                    relations: Set[String]
                  )
