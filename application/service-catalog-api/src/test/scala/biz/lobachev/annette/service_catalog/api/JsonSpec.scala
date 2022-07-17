package biz.lobachev.annette.service_catalog.api

import biz.lobachev.annette.core.model.auth.PersonPrincipal
import biz.lobachev.annette.service_catalog.api.common.{FileIcon, FrameworkIcon}
import biz.lobachev.annette.service_catalog.api.group.Group
import biz.lobachev.annette.service_catalog.api.item.{ExternalLink, InternalLink, Service}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import java.time.OffsetDateTime

class JsonSpec extends AnyWordSpec with Matchers {

  "Group" should {
    "group" in {
      val group = Group(
        id = "MAIN",
        name = "Основные сервисы",
        description = "Группа включает основные сервисы платформы Annette",
        icon = FrameworkIcon("house"),
        label = Map(
          "ru" -> "Основные сервисы",
          "en" -> "Main Services"
        ),
        labelDescription = Map(
          "ru" -> "Основные сервисы Annette",
          "en" -> "Annette main services"
        ),
        services = Seq(
          "ORG_STRUCTURE_SVC",
          "PERSON_SVC",
          "ANNETTE_EXT_SVC"
        ),
        active = true,
        updatedBy = PersonPrincipal("P0001"),
        updatedAt = OffsetDateTime.now()
      )
      val json  = Json.toJson(group)
      println(Json.prettyPrint(Json.toJson(json)))
      println()

    }
  }

  "Service" should {
    "service" in {
      val services = Seq(
        Service(
          id = "ORG_STRUCTURE_SVC",
          name = "Орг структура",
          description = "",
          icon = FrameworkIcon("house"),
          label = Map(
            "en" -> "Org. structure"
          ),
          labelDescription = Map(
            "en" -> "Annette org. structure service"
          ),
          link = InternalLink(
            applicationId = "ANNETE",
            url = "/org-structure/organizations",
            openInNew = false
          ),
          active = true,
          updatedBy = PersonPrincipal("P0001"),
          updatedAt = OffsetDateTime.now()
        ),
        Service(
          id = "PERSON_SVC",
          name = "Персоны",
          description = "",
          icon = FrameworkIcon("person"),
          label = Map(
            "en" -> "Persons"
          ),
          labelDescription = Map(
            "en" -> "Annette person service"
          ),
          link = InternalLink(
            applicationId = "ANNETTE",
            url = "/person/persons",
            openInNew = false
          ),
          active = true,
          updatedBy = PersonPrincipal("P0001"),
          updatedAt = OffsetDateTime.now()
        ),
        Service(
          id = "ANNETTE_EXT_SVC",
          name = "Сайт платформы Annette",
          description = "",
          icon = FileIcon("https://annetteplatform.github.io/annette_logo.svg"),
          label = Map(
            "en" -> "Annette site"
          ),
          labelDescription = Map(
            "en" -> "Annette site"
          ),
          link = ExternalLink(
            url = "https://annetteplatform.github.io/",
            openInNew = true
          ),
          active = true,
          updatedBy = PersonPrincipal("P0001"),
          updatedAt = OffsetDateTime.now()
        )
      )
      val json     = Json.toJson(services)
      println(Json.prettyPrint(Json.toJson(json)))
      println()

    }
  }
}
