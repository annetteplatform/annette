package biz.lobachev.annette.camunda4s

import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest}

class CamundaClient(url: String, credentials: Option[CamundaCredentials], ws: WSClient) {
  def request(api: String): WSRequest = {
    val r1 = ws.url(s"$url$api")
    credentials.map(cr => r1.withAuth(cr.login, cr.password, WSAuthScheme.BASIC)).getOrElse(r1)
  }
}
