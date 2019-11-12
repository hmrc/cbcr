/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.cbcr.models.{CorrespondenceDetails, MigrationRequest, SubscriptionRequest}
import uk.gov.hmrc.play.audit.model.Audit

import scala.concurrent.{ExecutionContext, Future, Promise}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpGet, HttpPost, HttpPut, HttpResponse}
import uk.gov.hmrc.http.logging.Authorization
import configs.syntax._
import uk.gov.hmrc.cbcr.services.RunMode
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
@ImplementedBy(classOf[DESConnectorImpl])
trait DESConnector extends RawResponseReads {

  implicit val ec: ExecutionContext
  implicit val configuration: Configuration
  implicit val runMode: RunMode

  def serviceUrl: String

  def orgLookupURI: String

  def cbcSubscribeURI: String

  def urlHeaderEnvironment: String

  def urlHeaderAuthorization: String

  def http: HttpPost with HttpGet with HttpPut

  val audit: Audit

  val lookupData: JsObject = Json.obj(
    "regime"            -> "ITSA",
    "requiresNameMatch" -> false,
    "isAnAgent"         -> false
  )

  val stubMigration: Boolean =
    configuration.underlying.get[Boolean](s"${runMode.env}.CBCId.stubMigration").valueOr(_ => false)

  val delayMigration: Int = 1000 * configuration.underlying
    .get[Int](s"${runMode.env}.CBCId.delayMigration")
    .valueOr(_ => 60)

  private def createHeaderCarrier: HeaderCarrier =
    HeaderCarrier(
      extraHeaders = Seq("Environment" -> urlHeaderEnvironment),
      authorization = Some(Authorization(urlHeaderAuthorization)))

  def lookup(utr: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    Logger.info(s"Lookup Request sent to DES: POST $serviceUrl/$orgLookupURI/utr/$utr")
    http.POST[JsValue, HttpResponse](s"$serviceUrl/$orgLookupURI/utr/$utr", Json.toJson(lookupData)).recover {
      case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
    }
  }

  def createSubscription(sub: SubscriptionRequest): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    implicit val writes = SubscriptionRequest.subscriptionWriter
    Logger.info(s"Create Request sent to DES: ${Json.toJson(sub)} for safeID: ${sub.safeId}")
    http.POST[SubscriptionRequest, HttpResponse](s"$serviceUrl/$cbcSubscribeURI", sub).recover {
      case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
    }
  }

  def createMigration(mig: MigrationRequest): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    implicit val writes = MigrationRequest.migrationWriter
    Logger.info(s"Migration Request sent to DES for safeId: ${mig.safeId} and CBCId: ${mig.cBCId}")

    Logger.warn(s"stubMigration set to: $stubMigration")
    val res = Promise[HttpResponse]()
    Future {
      if (!stubMigration) {
        Logger.info("calling ETMP for migration")
        Thread.sleep(delayMigration)
        http
          .POST[MigrationRequest, HttpResponse](s"$serviceUrl/$cbcSubscribeURI", mig)
          .recover {
            case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
          }
          .map(r => {
            Logger.info(s"Migration Status for safeId: ${mig.safeId} and cBCId: ${mig.cBCId} ${r.status}")
            res.success(r)
          })
      } else {
        Logger.info("in migration stub")

        Thread.sleep(delayMigration)
        res.success(HttpResponse(200, responseString = Some(s"migrated ${mig.cBCId}")))
      }
    }
    res.future
  }

  def updateSubscription(safeId: String, cor: CorrespondenceDetails): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    implicit val format = CorrespondenceDetails.updateWriter
    Logger.info(s"Update Request sent to DES: $cor for safeID: $safeId")
    http.PUT[CorrespondenceDetails, HttpResponse](s"$serviceUrl/$cbcSubscribeURI/$safeId", cor).recover {
      case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
    }
  }

  def getSubscription(safeId: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    Logger.info(s"Get Request sent to DES for safeID: $safeId")
    http.GET[HttpResponse](s"$serviceUrl/$cbcSubscribeURI/$safeId").recover {
      case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
    }
  }

}

@Singleton
class DESConnectorImpl @Inject()(
  val ec: ExecutionContext,
  val auditConnector: AuditConnector,
  val configuration: Configuration,
  val runMode: RunMode,
  val httpClient: HttpClient,
  val servicesConfig: ServicesConfig)
    extends DESConnector {
  lazy val serviceUrl: String = servicesConfig.baseUrl("etmp-hod")
  lazy val orgLookupURI: String = "registration/organisation"
  lazy val cbcSubscribeURI: String = "country-by-country/subscription"
  lazy val urlHeaderEnvironment: String = servicesConfig.getConfString("etmp-hod.environment", "")
  lazy val urlHeaderAuthorization: String =
    s"Bearer ${servicesConfig.getConfString("etmp-hod.authorization-token", "")}"
  val audit = new Audit("known-fact-checking", auditConnector)
  val http = httpClient
}
