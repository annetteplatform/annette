package biz.lobachev.annette.service_catalog.impl.common

import biz.lobachev.annette.microservice_core.db.QuillEncoders
import biz.lobachev.annette.service_catalog.api.common.Icon

trait IconEncoder extends QuillEncoders {

  implicit val encoder = genericJsonEncoder[Icon]
  implicit val decoder = genericJsonDecoder[Icon]
}
