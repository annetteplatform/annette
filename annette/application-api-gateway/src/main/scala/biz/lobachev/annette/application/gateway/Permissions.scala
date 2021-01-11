package biz.lobachev.annette.application.gateway

import biz.lobachev.annette.core.model.auth.Permission

object Permissions {
  final val MAINTAIN_ALL_APPLICATIONS = Permission("annette.application.application.maintain.all")
  final val MAINTAIN_ALL_TRANSLATIONS = Permission("annette.application.translation.maintain.all")
  final val MAINTAIN_ALL_LANGUAGES    = Permission("annette.application.language.maintain.all")
}
