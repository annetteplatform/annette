package biz.lobachev.annette.service_catalog.impl.common

import biz.lobachev.annette.core.model.translation.Caption
import biz.lobachev.annette.microservice_core.db.QuillEncoders

trait CaptionEncoder extends QuillEncoders {

  implicit val captionEncoder = genericJsonEncoder[Caption]
  implicit val captionDecoder = genericJsonDecoder[Caption]
}
