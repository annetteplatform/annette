package biz.lobachev.annette.blogs.api.post

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[MarkdownContent], name = "markdown"),
    new JsonSubTypes.Type(value = classOf[HtmlContent], name = "html")
  )
)
sealed trait PostContent

case class MarkdownContent(markdown: String) extends PostContent

object MarkdownContent {
  implicit val format = Json.format[MarkdownContent]
}

case class HtmlContent(html: String) extends PostContent

object HtmlContent {
  implicit val format = Json.format[HtmlContent]
}

object PostContent {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "MarkdownContent" => "markdown"
        case "HtmlContent"     => "html"
      }
    }
  )
  implicit val format = Json.format[PostContent]
}
