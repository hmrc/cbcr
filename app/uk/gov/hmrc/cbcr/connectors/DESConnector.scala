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
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.ws.WSPost
import uk.gov.hmrc.play.http.{HeaderCarrier, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


  @ImplementedBy(classOf[DESConnectorImpl])
  trait DESConnector extends ServicesConfig with RawResponseReads {

    implicit val ec:ExecutionContext

    def serviceUrl: String

    def orgLookupURI: String

    def urlHeaderEnvironment: String

    def urlHeaderAuthorization: String

    def http: HttpPost

    val audit:Audit

    val lookupData: JsObject = Json.obj(
      "regime" -> "ITSA",
      "requiresNameMatch" -> false,
      "isAnAgent" -> false
    )

    def lookup(utr: String): Future[HttpResponse] = {
      implicit val hc: HeaderCarrier = createHeaderCarrier
      http.POST[JsValue, HttpResponse](s"$serviceUrl/$orgLookupURI/utr/$utr", Json.toJson(lookupData)).map { response =>
        if(response.status != OK) {
          Logger.warn(s"[DESConnector][lookup] - status: ${response.status}")
          doFailedAudit("lookupFailed", lookupData.toString, response.body)
        }
        response
      }
    }

    def createHeaderCarrier: HeaderCarrier =
      HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))

    def doFailedAudit(auditType: String, request: String, response: String)(implicit hc:HeaderCarrier): Unit = {
      val auditDetails = Map("request" -> request,
        "response" -> response)

      audit.sendDataEvent(DataEvent("business-matching", auditType,
        tags = hc.toAuditTags("", "N/A"),
        detail = hc.toAuditDetails(auditDetails.toSeq: _*)))
    }
  }

  @Singleton
  class DESConnectorImpl @Inject() (val ec: ExecutionContext, val auditConnector:AuditConnectorI) extends DESConnector {
    lazy val serviceUrl: String = baseUrl("etmp-hod")
    lazy val orgLookupURI: String = "registration/organisation"
    lazy val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
    lazy val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"
    val audit = new Audit("known-fact-checking", auditConnector)
    val http:WSPost = new WSPost {
      override val hooks: Seq[HttpHook] = NoneRequired
    }
  }