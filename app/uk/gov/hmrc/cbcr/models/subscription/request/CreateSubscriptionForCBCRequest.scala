/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.models.subscription.request

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import uk.gov.hmrc.cbcr.models.subscription.request

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class CreateSubscriptionForCBCRequest(
  subscription: SubscriptionForCBCRequest,
  subscriptionID: String,
  lastUpdated: LocalDateTime = LocalDateTime.now
)

object CreateSubscriptionForCBCRequest {

  lazy val localDateTimeWrites: Writes[LocalDateTime] =
    Writes.apply[LocalDateTime] { date =>
      JsString(date.format(DateTimeFormatter.ISO_DATE_TIME))
    }

  val format: OFormat[CreateSubscriptionForCBCRequest] = OFormat(reads, writes)

  implicit lazy val reads: Reads[CreateSubscriptionForCBCRequest] = {
    import play.api.libs.functional.syntax._
    (
      (__ \\ "subscription").read[SubscriptionForCBCRequest] and
        (__ \\ "subscriptionID").read[String] and
        (__ \\ "lastUpdated").readNullable[LocalDateTime]
    )(
      (subscription, subscriptionID, lastUpdated) =>
        request.CreateSubscriptionForCBCRequest(subscription, subscriptionID, lastUpdated.getOrElse(LocalDateTime.now))
    )
  }

  implicit lazy val writes: OWrites[CreateSubscriptionForCBCRequest] = (
    (__ \ "subscription").write[SubscriptionForCBCRequest] and
      (__ \ "subscriptionID").write[String] and
      (__ \ "lastUpdated").write[LocalDateTime](localDateTimeWrites)
  )(
    r => (r.subscription, r.subscriptionID, r.lastUpdated)
  )
}
