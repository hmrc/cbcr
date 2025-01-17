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

package uk.gov.hmrc.cbcr.models

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.cbcr.emailaddress.EmailAddress
import uk.gov.hmrc.cbcr.emailaddress.PlayJsonFormats._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PhoneNumber private (val number: String)

//Must match telephone type from API docs
//https://[conflence]/display/CBC/DES+API+Specifications?preview=/89272679/89272676/API%20%231%20Subscription%20Create-v41-20170731.docx
object PhoneNumber {

  val pattern = "^[A-Z0-9 )/(-*#]+$"

  def apply(number: String): Option[PhoneNumber] =
    if (number.matches(pattern)) {
      Some(new PhoneNumber(number))
    } else {
      None
    }

  implicit val format: Format[PhoneNumber] = new Format[PhoneNumber] {
    override def writes(o: PhoneNumber): JsString = JsString(o.number)

    override def reads(json: JsValue): JsResult[PhoneNumber] = json match {
      case JsString(v) =>
        PhoneNumber(v).fold[JsResult[PhoneNumber]](
          JsError(s"Unable to serialise $json as a PhoneNumber")
        )(pn => JsSuccess(pn))
      case _ => JsError(s"Unable to serialise $json as a PhoneNumber")
    }
  }
}

case class ContactDetails(email: EmailAddress, phoneNumber: PhoneNumber)

object ContactDetails {

  val emailFormat: Format[EmailAddress] = new Format[EmailAddress] {
    override def writes(o: EmailAddress): JsObject = Json.obj("emailAddress" -> o.value)

    override def reads(json: JsValue): JsResult[EmailAddress] = json match {
      case JsObject(m) =>
        m.get("emailAddress")
          .flatMap(_.asOpt[String].map(EmailAddress(_)))
          .fold[JsResult[EmailAddress]](
            JsError("Unable to serialise emailAddress")
          )(emailAddress => JsSuccess(emailAddress))
      case other => JsError(s"Unable to serialise emailAddress: $other")
    }
  }
  implicit val format: OFormat[ContactDetails] = Json.format[ContactDetails]
}

case class ContactName(name1: String, name2: String)

object ContactName {
  implicit val format: OFormat[ContactName] = Json.format[ContactName]
}

case class CorrespondenceDetails(contactAddress: EtmpAddress, contactDetails: ContactDetails, contactName: ContactName)

object CorrespondenceDetails {
  implicit val format: OFormat[CorrespondenceDetails] = Json.format[CorrespondenceDetails]
  implicit val updateWriter: Writes[CorrespondenceDetails] = (o: CorrespondenceDetails) =>
    Json.obj(
      "correspondenceDetails" -> Json.obj(
        "contactAddress" -> EtmpAddress.formats.writes(o.contactAddress),
        "contactDetails" -> Json.obj(
          "emailAddress" -> o.contactDetails.email.value,
          "phoneNumber"  -> o.contactDetails.phoneNumber.number
        ),
        "contactName" -> ContactName.format.writes(o.contactName)
      )
    )
}

case class SubscriptionRequest(safeId: String, isMigrationRecord: Boolean, correspondenceDetails: CorrespondenceDetails)

object SubscriptionRequest {
  implicit val subscriptionWriter: Writes[SubscriptionRequest] = (o: SubscriptionRequest) =>
    Json.obj(
      "safeId"            -> o.safeId,
      "isMigrationRecord" -> o.isMigrationRecord,
      "correspondenceDetails" -> Json.obj(
        "contactAddress" -> EtmpAddress.formats.writes(o.correspondenceDetails.contactAddress),
        "contactDetails" -> Json.obj(
          "emailAddress" -> o.correspondenceDetails.contactDetails.email.value,
          "phoneNumber"  -> o.correspondenceDetails.contactDetails.phoneNumber.number
        ),
        "contactName" -> ContactName.format.writes(o.correspondenceDetails.contactName)
      )
    )
}

case class SubscriptionResponse(processingDate: LocalDateTime, cbcSubscriptionID: CBCId)

object SubscriptionResponse {
  private val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'")
  implicit val format: Writes[SubscriptionResponse] = (o: SubscriptionResponse) =>
    Json.obj(
      "processingDate"    -> o.processingDate.format(formatter),
      "cbcSubscriptionID" -> o.cbcSubscriptionID
    )

  implicit val reads: Reads[SubscriptionResponse] =
    ((JsPath \ "processingDate").read[LocalDateTime] and
      (JsPath \ "cbcSubscriptionID").read[CBCId])(SubscriptionResponse.apply _)

}
case class UpdateResponse(processingDate: LocalDateTime)
object UpdateResponse {
  private val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'")
  implicit val format: Writes[UpdateResponse] = (o: UpdateResponse) =>
    Json.obj(
      "processingDate" -> o.processingDate.format(formatter)
    )

  implicit val reads: Reads[UpdateResponse] = (JsPath \ "processingDate").read[LocalDateTime].map(UpdateResponse(_))
}

case class GetResponse(safeId: String, names: ContactName, contact: ContactDetails, address: EtmpAddress)
object GetResponse {

  implicit val contactReads: Reads[ContactDetails] =
    ((JsPath \ "email").read[EmailAddress] and (JsPath \ "phoneNumber").read[PhoneNumber])(ContactDetails.apply _)

  val grReads: Reads[GetResponse] = ((JsPath \ "safeId").read[String] and
    (JsPath \ "names").read[ContactName] and
    (JsPath \ "contact").read[ContactDetails] and
    (JsPath \ "address" \ "line1").read[String] and
    (JsPath \ "address" \ "line2").readNullable[String] and
    (JsPath \ "address" \ "line3").readNullable[String] and
    (JsPath \ "address" \ "line4").readNullable[String] and
    (JsPath \ "address" \ "postalCode").readNullable[String] and
    (JsPath \ "address" \ "countryCode")
      .read[String])((safeId, names, contact, line1, line2, line3, line4, postalCode, countryCode) =>
    GetResponse(safeId, names, contact, EtmpAddress(line1, line2, line3, line4, postalCode, countryCode))
  )

  implicit val format: Format[GetResponse] = new Format[GetResponse] {
    override def writes(o: GetResponse): JsObject = Json.obj(
      "safeId"  -> o.safeId,
      "names"   -> ContactName.format.writes(o.names),
      "contact" -> ContactDetails.format.writes(o.contact),
      "address" -> Json.obj(
        "line1"       -> o.address.addressLine1,
        "line2"       -> o.address.addressLine2,
        "line3"       -> o.address.addressLine3,
        "line4"       -> o.address.addressLine4,
        "postalCode"  -> o.address.postalCode,
        "countryCode" -> o.address.countryCode
      )
    )

    override def reads(json: JsValue): JsResult[GetResponse] = grReads.reads(json)
  }

}
