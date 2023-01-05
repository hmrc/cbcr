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

import org.mongodb.scala.model.Filters

import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.cbcr.models.{CBCId, SubscriptionDetails}
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class AuditSubscriptionService @Inject()(
  repo: SubscriptionDataRepository,
  configuration: Configuration,
  runMode: RunMode,
  audit: AuditConnector)(implicit ex: ExecutionContext) {

  lazy val logger: Logger = Logger(this.getClass)

  val auditSubscriptions: Boolean =
    configuration.getOptional[Boolean](s"${runMode.env}.audit.subscriptions").getOrElse(false)
  logger.info(s"auditSubscriptions set to: $auditSubscriptions")

  if (auditSubscriptions) {
    val cbcIds: List[CBCId] =
      configuration
        .getOptional[String](s"${runMode.env}.audit.cbcIds")
        .getOrElse("")
        .split("_")
        .toList
        .flatMap(CBCId.apply)

    Await.result(
      repo
        .getSubscriptions(Filters.in("cbcId", cbcIds: _*))
        .map(sd =>
          sd.foreach(s =>
            auditSubscriptionDetails(s).onComplete {
              case Success(AuditResult.Success) =>
                logger.info(s"Successfully audited SubscriptionDetails of CBCId ${s.cbcId.toString}")
              case Success(AuditResult.Failure(msg, _)) =>
                logger.warn(s"Unable to audit SubscriptionDetails of CBCId ${s.cbcId.toString} $msg")
              case Success(AuditResult.Disabled) =>
                logger.warn(s"Auditing is disabled for SubscriptionDetails of CBCId ${s.cbcId.toString}")
              case Failure(e) => logger.warn(s"Audit failed to complete for CBCId ${s.cbcId.toString}, ${e.getMessage}")
          })),
      Duration(60, "second")
    )

  }

  private def auditSubscriptionDetails(sd: SubscriptionDetails): Future[AuditResult] =
    audit.sendExtendedEvent(
      ExtendedDataEvent(
        auditSource = "Country-By-Country",
        "SubscriptionDetails",
        detail = Json.obj(
          "cbcId"               -> JsString(sd.cbcId.toString),
          "SubscriptionDetails" -> Json.toJson(sd)
        )))
}
