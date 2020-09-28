import biz.lobachev.annette.attributes.api.attribute_def.{AttributeDef, AttributeType}
import biz.lobachev.annette.core.model.AnnettePrincipal
import play.api.libs.json.Json

val attr = AttributeDef(
    id = "sex",
    name = "Sex",
    caption = "Sex",
    attributeType = AttributeType.String,
    attributeId = "sex",
    subType = None,
    allowedValues = Map("M" -> "Male", "F" -> "Female", "AH" -> "Attacking helocopter"),
    updatedBy = AnnettePrincipal("person", "valery")
  )

val json = Json.toJson(attr)

println(Json.prettyPrint(json))
