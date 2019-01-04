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

package uk.gov.hmrc.cbcr.services

import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.cbcr.models.{CBCId, SubscriptionDetails}
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AuditSubscriptionService @Inject() (repo:SubscriptionDataRepository,
                                          configuration:Configuration,
                                          runMode: RunMode,
                                          audit: AuditConnectorI) (implicit ex: ExecutionContext) {

  val auditSubscriptions: Boolean = configuration.getBoolean(s"${runMode.env}.audit.subscriptions").getOrElse(false)
  Logger.info(s"auditSubscriptions set to: $auditSubscriptions")

  if (auditSubscriptions) {
    val cbcIds: List[CBCId] = configuration.getString(s"${runMode.env}.audit.cbcIds").getOrElse("").split("_").toList.flatMap(CBCId.apply)
    val subscriptions = Json.obj("cbcId" -> Json.obj("$in" -> cbcIds))

    repo.getSubscriptions(subscriptions).map(sd => sd.foreach( s => auditSubscriptionDetails(s).onComplete {
      case Success(AuditResult.Success) => Logger.info(s"Successfully audited SubscriptionDetails of CBCId ${s.cbcId.toString}")
      case Success(AuditResult.Failure(msg, _)) => Logger.warn(s"Unable to audit SubscriptionDetails of CBCId ${s.cbcId.toString} $msg")
      case Success(AuditResult.Disabled) => Logger.warn(s"Auditing is disabled for SubscriptionDetails of CBCId ${s.cbcId.toString}")
      case Failure(e) => Logger.warn(s"Audit failed to complete for CBCId ${s.cbcId.toString}, ${e.getMessage}")
    }))

  }

  private def auditSubscriptionDetails(sd: SubscriptionDetails): Future[AuditResult] = {
    audit.sendExtendedEvent(ExtendedDataEvent(auditSource = "Country-By-Country", "SubscriptionDetails",
      detail = Json.obj(
        "cbcId"        -> JsString(sd.cbcId.toString),
        "SubscriptionDetails" -> Json.toJson(sd)
      )
    ))
  }
}
