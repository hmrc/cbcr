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

package uk.gov.hmrc.cbcr.models

import play.api.libs.json._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.emailaddress.PlayJsonFormats._
import play.api.libs.functional.syntax._ // Combinator syntax

case class SubscriberContact(
  name: Option[String],
  firstName: String,
  lastName: String,
  phoneNumber: PhoneNumber,
  email: EmailAddress)
object SubscriberContact {

  implicit val formats: Format[SubscriberContact] = Json.format[SubscriberContact]

  val subscriberContactFormat = new Format[SubscriberContact] {
    override def writes(o: SubscriberContact) = Json.obj(
      "name"        -> o.name,
      "firstName"   -> o.firstName,
      "lastName"    -> o.lastName,
      "phoneNumber" -> o.phoneNumber,
      "email"       -> o.email
    )

    implicit val subscriberContactReads: Reads[SubscriberContact] =
      ((JsPath \ "name").readNullable[String] and
        (JsPath \ "firstName").read[String] and
        (JsPath \ "lastName").read[String] and
        (JsPath \ "phoneNumber").read[PhoneNumber] and
        (JsPath \ "email").read[EmailAddress])(SubscriberContact.apply _)

    override def reads(json: JsValue) = subscriberContactReads.reads(json)
  }

}

case class SubscriptionDetails(
  businessPartnerRecord: BusinessPartnerRecord,
  subscriberContact: SubscriberContact,
  cbcId: Option[CBCId],
  utr: Utr)
object SubscriptionDetails {
  implicit val subscriptionDetailsFormat = Json.format[SubscriptionDetails]
}
