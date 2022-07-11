package biz.lobachev.annette.service_catalog.api.common

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[FileIcon], name = "file"),
    new JsonSubTypes.Type(value = classOf[FrameworkIcon], name = "framework")
  )
)
sealed trait Icon

case class FileIcon(
  url: String
) extends Icon

object FileIcon {
  implicit val format = Json.format[FileIcon]
}

case class FrameworkIcon(
  icon: String
) extends Icon

object FrameworkIcon {
  implicit val format = Json.format[FrameworkIcon]
}

object Icon {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "FileIcon"      => "file"
        case "FrameworkIcon" => "framework"
      }
    }
  )

  implicit val format = Json.format[Icon]
}
