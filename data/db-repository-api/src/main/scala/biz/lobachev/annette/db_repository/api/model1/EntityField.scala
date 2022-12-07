package biz.lobachev.annette.db_repository.api.model1

case class EntityField(
  id: String,
  name: String,
  description: String,
  fieldName: String,
  dbFieldName: String,
  domain: String,
  notNull: Boolean,
  order: Int
)
