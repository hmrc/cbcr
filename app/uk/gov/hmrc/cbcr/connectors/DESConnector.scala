/*
 * Copyright 2017 HM Revenue & Customs
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
//import com.oracle.tools.packager.Log.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.cbcr.models.{SubscriptionRequestBody, SubscriptionRequestBody2}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.ws.WSPost
import uk.gov.hmrc.play.http.{HeaderCarrier, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger


  @ImplementedBy(classOf[DESConnectorImpl])
  trait DESConnector extends ServicesConfig with RawResponseReads {

    implicit val ec:ExecutionContext

    def serviceUrl: String

    def orgLookupURI: String

    def cbcSubscribeURI: String

    def urlHeaderEnvironment: String

    def urlHeaderAuthorization: String

    def http: HttpPost

    val audit:Audit

    val lookupData: JsObject = Json.obj(
      "regime" -> "ITSA",
      "requiresNameMatch" -> false,
      "isAnAgent" -> false
    )

    val srb: JsObject = Json.obj(
      "safeId" -> "XA0000000012345",
      "isMigrationRecord" -> false,
      "correspondenceDetails" -> Json.obj(
        "contactAddress" -> Json.obj(
          "addressLine1" -> "Matheson House 56",
          "addressLine2" -> "Grange Central 56",
          "postalCode" -> "TF3 4ER",
          "countryCode" -> "GB")
        ,
        "contactDetails" -> Json.obj(
          "emailAddress" -> "fred_flintstone@hotmail.com",
          "phoneNumber" -> "011 555 30440")
        ,
        "contactName"-> Json.obj(
          "name1" -> "Fred",
          "name2" -> "Flintstone")
      )
    )

    private def createHeaderCarrier: HeaderCarrier =
      HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))

    def lookup(utr: String): Future[HttpResponse] = {
      implicit val hc: HeaderCarrier = createHeaderCarrier
      http.POST[JsValue, HttpResponse](s"$serviceUrl/$orgLookupURI/utr/$utr", Json.toJson(lookupData)).recover{
        case e:HttpException => HttpResponse(e.responseCode,responseString = Some(e.message))
      }
    }

    def subscribeToCBC(sub:SubscriptionRequestBody2)(implicit hc:HeaderCarrier) : Future[HttpResponse] = {
      val tempJson = Json.toJson(srb)
      Logger.info(s"JsObject sent to DES: $tempJson")
      http.POST[JsValue, HttpResponse](s"$serviceUrl/$cbcSubscribeURI", Json.toJson(sub)).recover{
        case e:HttpException => HttpResponse(e.responseCode,responseString = Some(e.message))
      }
    }

//    def subscribeToCBC(sub:SubscriptionRequestBody2)(implicit hc:HeaderCarrier) : Future[HttpResponse] = {
//      Logger.info(s"JsObject sent to DES: $sub")
//      http.POST[SubscriptionRequestBody2, HttpResponse](s"$serviceUrl/$cbcSubscribeURI", sub).recover{
//        case e:HttpException =>
//          Logger.info(s"Subscribe returned an exception: ${e.getMessage}")
//          HttpResponse(e.responseCode,responseString = Some(e.message))
//      }
//    }

  }

  @Singleton
  class DESConnectorImpl @Inject() (val ec: ExecutionContext, val auditConnector:AuditConnectorI) extends DESConnector {
    lazy val serviceUrl: String = baseUrl("etmp-hod")
    lazy val orgLookupURI: String = "registration/organisation"
    lazy val cbcSubscribeURI: String = "country-by-country/subscription"
    lazy val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
    lazy val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"
    val audit = new Audit("known-fact-checking", auditConnector)
    val http:WSPost = new WSPost {
      override val hooks: Seq[HttpHook] = NoneRequired
    }
  }
