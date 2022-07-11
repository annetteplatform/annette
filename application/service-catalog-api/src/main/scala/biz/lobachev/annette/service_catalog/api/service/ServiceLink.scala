package biz.lobachev.annette.service_catalog.api.service

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ExternalLink], name = "external"),
    new JsonSubTypes.Type(value = classOf[InternalLink], name = "internal")
  )
)
sealed trait ServiceLink {
  val url: String
  val openInNew: Boolean
}

case class ExternalLink(
  url: String,
  openInNew: Boolean
) extends ServiceLink

object ExternalLink {
  implicit val format = Json.format[ExternalLink]
}

case class InternalLink(
  applicationId: String,
  url: String,
  openInNew: Boolean
) extends ServiceLink

object InternalLink {
  implicit val format = Json.format[InternalLink]
}

object ServiceLink {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "ExternalLink" => "external"
        case "InternalLink" => "internal"
      }
    }
  )

  implicit val format = Json.format[ServiceLink]
}
