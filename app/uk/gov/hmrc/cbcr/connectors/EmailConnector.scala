/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.cbcr.connectors

import com.google.inject.ImplementedBy
import com.typesafe.config.Config
import play.api.Configuration
import uk.gov.hmrc.cbcr.models.Email
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import uk.gov.hmrc.http._

@ImplementedBy(classOf[EmailConnectorImpl])
trait EmailConnector {
  val serviceUrl: String
  val port: String
  val host: String
  val protocol: String

}

@Singleton
class EmailConnectorImpl @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit ec: ExecutionContext)
    extends EmailConnector {
  private val conf: Config = config.underlying.getConfig("microservice.services.email")
  val host: String = conf.getString("host")
  val port: String = conf.getInt("port").toString
  val protocol: String = conf.getString("protocol")
  val serviceUrl = s"$protocol://$host:$port/hmrc"

  def sendEmail(email: Email)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient.post(url"$serviceUrl/email/").withBody(Json.toJson(email)).execute[HttpResponse]
}
