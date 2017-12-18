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

package uk.gov.hmrc.cbcr.services

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.cbcr.connectors.EmailConnectorImpl
import uk.gov.hmrc.cbcr.models.Email
import play.api.mvc.Results._
import uk.gov.hmrc.cbcr.audit.AuditConnectorI

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditResult

@Singleton
class EmailService @Inject()(emailConnector:EmailConnectorImpl, auditConnector:AuditConnectorI) {

  private val ALERT_GENERATION_STRING_TO_CREATE_PAGER_DUTY = "**CBCR_EMAIL_FAILURE**"

  def sendEmail(email:Email)(implicit hc:HeaderCarrier):Future[Result] = {
    emailConnector.sendEmail(email).map(res => res.status match {
      case 202 =>
        Logger.info("CBCR Successfully sent email")
        audit(email, CBCREmailSuccess)
        Accepted
    }).recover {
      case _ =>
        Logger.error(ALERT_GENERATION_STRING_TO_CREATE_PAGER_DUTY)
        audit(email, CBCREmailFailure)
        BadRequest
    }
  }

  def audit(email:Email, auditType:AuditType)(implicit hc:HeaderCarrier) = {
    auditConnector.sendExtendedEvent(ExtendedDataEvent(auditSource = "Country-By-Country", auditType.toString,
      tags = hc.toAuditTags(auditType.toString, "N/A") ++ Map("path" -> emailConnector.serviceUrl),
      detail = Json.toJson(email)
    )).map {
      case AuditResult.Success => Logger.info(s"Successfully audited ${auditType.toString}")
      case AuditResult.Failure(msg, _) => Logger.warn(s"Unable to audit ${auditType.toString} $msg")
      case AuditResult.Disabled => Logger.warn(s"Auditing is disabled for ${auditType.toString}")
    }
  }

}

sealed trait AuditType
  case object CBCREmailFailure extends AuditType
  case object CBCREmailSuccess extends AuditType
