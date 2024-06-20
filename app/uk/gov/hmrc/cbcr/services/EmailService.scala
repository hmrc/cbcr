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

package uk.gov.hmrc.cbcr.services

import javax.inject.{Inject, Singleton}
import play.api.{Logger}
import play.api.libs.json.{JsString, Json}
import play.api.mvc.Result
import uk.gov.hmrc.cbcr.connectors.EmailConnectorImpl
import uk.gov.hmrc.cbcr.models.Email
import play.api.mvc.Results._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

@Singleton
class EmailService @Inject() (emailConnector: EmailConnectorImpl, auditConnector: AuditConnector)(implicit
  val ec: ExecutionContext
) {

  lazy val logger: Logger = Logger(this.getClass)

  def sendEmail(email: Email)(implicit hc: HeaderCarrier): Future[Result] =
    emailConnector
      .sendEmail(email)
      .map(res =>
        res.status match {
          case 202 =>
            logger.info("CBCR Successfully sent email")
            audit(email, CBCREmailSuccess)
            Accepted
          case _ =>
            throw new RuntimeException("Unexpected status")
        }
      )
      .recover { case _ =>
        logger.error("Failed to send e-mail")
        audit(email, CBCREmailFailure)
        BadRequest
      }

  def audit(email: Email, auditType: AuditType)(implicit hc: HeaderCarrier) =
    auditConnector
      .sendExtendedEvent(
        ExtendedDataEvent(
          auditSource = "Country-By-Country",
          auditType.toString,
          detail = Json.obj(
            "email" -> Json.toJson(email),
            "path"  -> JsString(emailConnector.serviceUrl)
          )
        )
      )
      .map {
        case AuditResult.Success         => logger.info(s"Successfully audited ${auditType.toString}")
        case AuditResult.Failure(msg, _) => logger.warn(s"Unable to audit ${auditType.toString} $msg")
        case AuditResult.Disabled        => logger.warn(s"Auditing is disabled for ${auditType.toString}")
      }

}

sealed trait AuditType
case object CBCREmailFailure extends AuditType
case object CBCREmailSuccess extends AuditType
