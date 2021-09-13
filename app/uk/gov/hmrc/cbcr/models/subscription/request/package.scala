/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.models.subscription

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

package object request {

  case class RequestParameter(paramName: String, paramValue: String)

  object RequestParameter {
    implicit val format: OFormat[RequestParameter] = Json.format[RequestParameter]
  }

  case class CreateRequestDetail(
    IDType: String,
    IDNumber: String,
    tradingName: Option[String],
    isGBUser: Boolean,
    primaryContact: PrimaryContact,
    secondaryContact: Option[SecondaryContact])

  object CreateRequestDetail {

    implicit val reads: Reads[CreateRequestDetail] = {
      import play.api.libs.functional.syntax._
      (
        (__ \ "IDType").read[String] and
          (__ \ "IDNumber").read[String] and
          (__ \ "tradingName").readNullable[String] and
          (__ \ "isGBUser").read[Boolean] and
          (__ \ "primaryContact").read[PrimaryContact] and
          (__ \ "secondaryContact").readNullable[SecondaryContact]
      )(
        (idType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact) =>
          CreateRequestDetail(idType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
      )
    }

    implicit val writes: OWrites[CreateRequestDetail] = Json.writes[CreateRequestDetail]
  }

  case class DisplayRequestDetail(IDType: String, IDNumber: String)

  object DisplayRequestDetail {
    implicit val format: OFormat[DisplayRequestDetail] = Json.format[DisplayRequestDetail]
  }

  case class RequestCommonForDisplay(
    regime: String,
    conversationID: Option[String],
    receiptDate: String,
    acknowledgementReference: String,
    originatingSystem: String,
    requestParameters: Option[Seq[RequestParameter]])

  object RequestCommonForDisplay {
    implicit val format: OFormat[RequestCommonForDisplay] = Json.format[RequestCommonForDisplay]

    def apply(): RequestCommonForDisplay = {
      //Format: ISO 8601 YYYY-MM-DDTHH:mm:ssZ e.g. 2020-09-23T16:12:11Z
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

      //Generate a 32 chars UUID without hyphens
      val acknowledgementReference = UUID.randomUUID().toString.replace("-", "")
      val conversationID = UUID.randomUUID().toString

      RequestCommonForDisplay(
        regime = "CBC",
        conversationID = Some(conversationID),
        receiptDate = ZonedDateTime.now().format(formatter),
        acknowledgementReference = acknowledgementReference,
        originatingSystem = "MDTP",
        requestParameters = None
      )
    }
  }

  case class RequestCommonForSubscription(
    regime: String,
    receiptDate: String,
    acknowledgementReference: String,
    originatingSystem: String,
    requestParameters: Option[Seq[RequestParameter]])

  object RequestCommonForSubscription {
    implicit val format = Json.format[RequestCommonForSubscription]
  }

  case class RequestCommonForUpdate(
    regime: String,
    receiptDate: String,
    acknowledgementReference: String,
    originatingSystem: String,
    requestParameters: Option[Seq[RequestParameter]])

  object RequestCommonForUpdate {
    implicit val format: OFormat[RequestCommonForUpdate] = Json.format[RequestCommonForUpdate]
  }

  case class RequestDetailForUpdate(
    IDType: String,
    IDNumber: String,
    tradingName: Option[String],
    isGBUser: Boolean,
    primaryContact: PrimaryContact,
    secondaryContact: Option[SecondaryContact])

  object RequestDetailForUpdate {

    implicit val reads: Reads[RequestDetailForUpdate] = (
      (__ \ "IDType").read[String] and
        (__ \ "IDNumber").read[String] and
        (__ \ "tradingName").readNullable[String] and
        (__ \ "isGBUser").read[Boolean] and
        (__ \ "primaryContact").read[PrimaryContact] and
        (__ \ "secondaryContact").readNullable[SecondaryContact]
    )(
      (idt, idr, tn, gb, pc, sc) => RequestDetailForUpdate(idt, idr, tn, gb, pc, sc)
    )

    implicit lazy val writes: Writes[RequestDetailForUpdate] = (
      (__ \ "IDType").write[String] and
        (__ \ "IDNumber").write[String] and
        (__ \ "tradingName").writeNullable[String] and
        (__ \ "isGBUser").write[Boolean] and
        (__ \ "primaryContact").write[PrimaryContact] and
        (__ \ "secondaryContact").writeNullable[SecondaryContact]
    )(
      r => (r.IDType, r.IDNumber, r.tradingName, r.isGBUser, r.primaryContact, r.secondaryContact)
    )
  }
}
