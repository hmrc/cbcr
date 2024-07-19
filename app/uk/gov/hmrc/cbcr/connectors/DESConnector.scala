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
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import uk.gov.hmrc.cbcr.config.ApplicationConfig
import uk.gov.hmrc.cbcr.models.{CorrespondenceDetails, SubscriptionRequest}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DESConnectorImpl])
trait DESConnector extends RawResponseReads with HttpErrorFunctions {

  lazy val logger: Logger = Logger(this.getClass)

  implicit val ec: ExecutionContext
  implicit val configuration: ApplicationConfig

  def serviceUrl: String

  def orgLookupURI: String

  def cbcSubscribeURI: String

  def urlHeaderEnvironment: String

  def urlHeaderAuthorization: String

  def http: HttpPost with HttpGet with HttpPut

  val audit: Audit

  private[connectors] def customDESRead(
    http: String,
    url: String,
    response: HttpResponse
  ): HttpResponse =
    response.status match {
      case 429 =>
        logger.error("[RATE LIMITED] Received 429 from DES - converting to 503")
        throw UpstreamErrorResponse("429 received from DES - converted to 503", 429, 503)
      case _ => response
    }

  implicit val httpRds: HttpReads[HttpResponse] = (http: String, url: String, res: HttpResponse) =>
    customDESRead(http, url, res)

  private val lookupData: JsObject = Json.obj(
    "regime"            -> "ITSA",
    "requiresNameMatch" -> false,
    "isAnAgent"         -> false
  )

  private def desHeaders = Seq("Environment" -> urlHeaderEnvironment, "Authorization" -> urlHeaderAuthorization)

  private def withCorrelationId[T](f: HeaderCarrier => T)(implicit hc: HeaderCarrier): T =
    f(hc.requestId match {
      case Some(requestId) => hc.withExtraHeaders("X-Correlation-ID" -> requestId.value)
      case None            => hc
    })

  def lookup(utr: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Lookup Request sent to DES")
    withCorrelationId { implicit hc =>
      http.POST[JsValue, HttpResponse](s"$serviceUrl/$orgLookupURI/utr/$utr", Json.toJson(lookupData), desHeaders)
    } recover { case e: HttpException =>
      HttpResponse(status = e.responseCode, body = e.message)
    }
  }

  def createSubscription(sub: SubscriptionRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val writes: Writes[SubscriptionRequest] = SubscriptionRequest.subscriptionWriter
    logger.info(s"Create Request sent to DES")
    withCorrelationId { implicit hc =>
      http.POST[SubscriptionRequest, HttpResponse](s"$serviceUrl/$cbcSubscribeURI", sub, desHeaders)
    } recover { case e: HttpException =>
      HttpResponse(status = e.responseCode, body = e.message)
    }
  }

  def updateSubscription(safeId: String, cor: CorrespondenceDetails)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    implicit val format: Writes[CorrespondenceDetails] = CorrespondenceDetails.updateWriter
    logger.info(s"Update Request sent to DES")
    withCorrelationId { implicit hc =>
      http.PUT[CorrespondenceDetails, HttpResponse](s"$serviceUrl/$cbcSubscribeURI/$safeId", cor, desHeaders)
    } recover { case e: HttpException =>
      HttpResponse(status = e.responseCode, body = e.message)
    }
  }

  def getSubscription(safeId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Get Request sent to DES for safeID: $safeId")
    withCorrelationId { implicit hc =>
      http.GET[HttpResponse](s"$serviceUrl/$cbcSubscribeURI/$safeId", headers = desHeaders)
    } recover { case e: HttpException =>
      HttpResponse(status = e.responseCode, body = e.message)
    }
  }

}

@Singleton
class DESConnectorImpl @Inject() (
  val ec: ExecutionContext,
  val auditConnector: AuditConnector,
  val configuration: ApplicationConfig,
  val httpClient: HttpClient
) extends DESConnector {
  lazy val serviceUrl: String = configuration.etmpHod
  lazy val orgLookupURI: String = "registration/organisation"
  lazy val cbcSubscribeURI: String = "country-by-country/subscription"
  lazy val urlHeaderEnvironment: String = configuration.etmpHodEnvironment
  lazy val urlHeaderAuthorization: String = s"Bearer ${configuration.etmpHodAuthorizationToken}"
  val audit = new Audit("known-fact-checking", auditConnector)
  val http: HttpPost with HttpGet with HttpPut = httpClient
}
