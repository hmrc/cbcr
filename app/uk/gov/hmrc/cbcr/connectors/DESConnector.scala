/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.{Logger, Configuration}
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.cbcr.models.{ContactDetails, CorrespondenceDetails, MigrationRequest, SubscriptionRequest}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.{WSGet, WSHttp, WSPost, WSPut}
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpGet, HttpPost, HttpPut, HttpResponse}
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.logging.Authorization
import configs.syntax._
import uk.gov.hmrc.cbcr.services.RunMode


  @ImplementedBy(classOf[DESConnectorImpl])
  trait DESConnector extends ServicesConfig with RawResponseReads {

    implicit val ec:ExecutionContext
    implicit val configuration:Configuration
    implicit val runMode:RunMode

    def serviceUrl: String

    def orgLookupURI: String

    def cbcSubscribeURI: String

    def urlHeaderEnvironment: String

    def urlHeaderAuthorization: String

    def http: HttpPost with HttpGet with HttpPut

    val audit:Audit

    val lookupData: JsObject = Json.obj(
      "regime" -> "ITSA",
      "requiresNameMatch" -> false,
      "isAnAgent" -> false
    )


    private def createHeaderCarrier: HeaderCarrier =
      HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))

    def lookup(utr: String): Future[HttpResponse] = {
      implicit val hc: HeaderCarrier = createHeaderCarrier
      Logger.info(s"Lookup Request sent to DES: POST $serviceUrl/$orgLookupURI/utr/$utr")
      http.POST[JsValue, HttpResponse](s"$serviceUrl/$orgLookupURI/utr/$utr", Json.toJson(lookupData)).recover{
        case e:HttpException => HttpResponse(e.responseCode,responseString = Some(e.message))
      }
    }

    def createSubscription(sub:SubscriptionRequest): Future[HttpResponse] = {
      implicit val hc: HeaderCarrier = createHeaderCarrier
      implicit val writes = SubscriptionRequest.subscriptionWriter
      Logger.info(s"Create Request sent to DES: ${Json.toJson(sub)} for safeID: ${sub.safeId}")
      http.POST[SubscriptionRequest, HttpResponse](s"$serviceUrl/$cbcSubscribeURI", sub).recover{
        case e:HttpException => HttpResponse(e.responseCode,responseString = Some(e.message))
      }
    }

    def createMigration(mig:MigrationRequest) : Future[HttpResponse] = {
      implicit val hc: HeaderCarrier = createHeaderCarrier
      implicit val writes = MigrationRequest.migrationWriter
      Logger.info(s"Migration Request sent to DES: ${Json.toJson(mig)} for CBCId: ${mig.cBCId}")

      val stubMigration: Boolean = configuration.underlying.get[Boolean](s"${runMode.env}.CBCId.stubMigration").valueOr(_ => false)
      if (!stubMigration) {
        http.POST[MigrationRequest, HttpResponse](s"$serviceUrl/$cbcSubscribeURI", mig).recover {
          case e: HttpException => HttpResponse(e.responseCode, responseString = Some(e.message))
        }
      } else {
            val delayMigration: Int = configuration.underlying.get[Int](s"${runMode.env}.CBCId.delayMigration").valueOr(_ => 60)
            Thread.sleep(1000 * delayMigration)
            Future.successful(HttpResponse(200,responseString = Some(s"migrated ${mig.cBCId}")))
      }
    }

    def updateSubscription(safeId:String,cor:CorrespondenceDetails) : Future[HttpResponse] = {
      implicit val hc: HeaderCarrier = createHeaderCarrier
      implicit val format = CorrespondenceDetails.updateWriter
      Logger.info(s"Update Request sent to DES: $cor for safeID: $safeId")
      http.PUT[CorrespondenceDetails, HttpResponse](s"$serviceUrl/$cbcSubscribeURI/$safeId", cor).recover{
        case e:HttpException => HttpResponse(e.responseCode,responseString = Some(e.message))
      }
    }

    def getSubscription(safeId:String):Future[HttpResponse] = {
      implicit val hc: HeaderCarrier = createHeaderCarrier
      Logger.info(s"Get Request sent to DES for safeID: $safeId")
      http.GET[HttpResponse](s"$serviceUrl/$cbcSubscribeURI/$safeId").recover{
        case e:HttpException => HttpResponse(e.responseCode,responseString = Some(e.message))
      }
    }


  }

  @Singleton
  class DESConnectorImpl @Inject() (val ec: ExecutionContext,
                                    val auditConnector:AuditConnectorI,
                                    val configuration:Configuration,
                                    val runMode:RunMode) extends DESConnector {
    lazy val serviceUrl: String = baseUrl("etmp-hod")
    lazy val orgLookupURI: String = "registration/organisation"
    lazy val cbcSubscribeURI: String = "country-by-country/subscription"
    lazy val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
    lazy val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"
    val audit = new Audit("known-fact-checking", auditConnector)
    val http = new  HttpPost with HttpGet with HttpPut with  WSGet with WSPost with WSPut{
      override val hooks: Seq[HttpHook] = NoneRequired
    }
  }
