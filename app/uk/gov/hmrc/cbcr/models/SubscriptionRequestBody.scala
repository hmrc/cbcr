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

package uk.gov.hmrc.cbcr.models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import play.api.libs.json._ // JSON library
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._ // Combinator syntax

import play.api.libs.json._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.emailaddress.PlayJsonFormats._

class PhoneNumber private(val number:String)

//Must match telephone type from API docs
//https://confluence.tools.tax.service.gov.uk/display/CBC/DES+API+Specifications?preview=/89272679/89272676/API%20%231%20Subscription%20Create-v41-20170731.docx
object PhoneNumber {

  val pattern = "^[A-Z0-9 )/(-*#]+$"

  def apply(number: String): Option[PhoneNumber] =
    if (number.matches(pattern)) {
      Some(new PhoneNumber(number))
    } else {
      None
    }

  implicit val format = new Format[PhoneNumber] {
    override def writes(o: PhoneNumber) = JsString(o.number)

    override def reads(json: JsValue) =json match {
      case JsString(v) => PhoneNumber(v).fold[JsResult[PhoneNumber]](
        JsError(s"Unable to serialise $json as a PhoneNumber")
      )(pn => JsSuccess(pn))
      case _           => JsError(s"Unable to serialise $json as a PhoneNumber")
    }
  }
}

case class ContactDetails(emailAddress:EmailAddress, phoneNumber:PhoneNumber)

object ContactDetails{
  implicit val format = Json.format[ContactDetails]
}

case class ContactName(name1:String,name2:String)

object ContactName {
  implicit val format = Json.format[ContactName]
}


case class CorrespondenceDetails(contactAddress: EtmpAddress,
                                 contactDetails:ContactDetails,
                                 contactName: ContactName)

object CorrespondenceDetails{
  implicit val format = Json.format[CorrespondenceDetails]
}

case class SubscriptionRequestBody(safeID:String, isMigrationRecord:Boolean, cbcRegNumber:Option[CBCId], correspondenceDetails: CorrespondenceDetails )

object SubscriptionRequestBody{
  implicit val format = Json.format[SubscriptionRequestBody]
}

case class SubscriptionRequestBody2(safeID:String, isMigrationRecord:Boolean, correspondenceDetails: CorrespondenceDetails )

object SubscriptionRequestBody2{
  implicit val format = Json.format[SubscriptionRequestBody2]
}


case class SubscriptionRequestResponse(processingDate:LocalDateTime, cbcSubscriptionID:CBCId)

object SubscriptionRequestResponse{
  val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss'Z'")
  implicit val format = new Writes[SubscriptionRequestResponse] {
    override def writes(o: SubscriptionRequestResponse) = Json.obj(
      "processingDate" -> o.processingDate.format(formatter),
      "cbcSubscriptionID" -> o.cbcSubscriptionID
    )
  }

  implicit val reads: Reads[SubscriptionRequestResponse] =
    ((JsPath \ "processingDate").read[LocalDateTime] and
      (JsPath \ "cbcSubscriptionID").read[CBCId])(SubscriptionRequestResponse.apply _)

}