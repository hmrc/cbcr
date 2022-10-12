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

package uk.gov.hmrc.cbcr.models.subscription

import org.scalatest.MustMatchers.convertToAnyMustWrapper
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.cbcr.models.subscription.request.{DisplayRequestDetail, DisplaySubscriptionDetails, DisplaySubscriptionForCBCRequest, RequestCommonForDisplay}
import uk.gov.hmrc.cbcr.util.SpecBase

import scala.util.matching.Regex

class DisplaySubscriptionForCBCRequestSpec extends SpecBase {

  val displayRequestDetail: DisplayRequestDetail = DisplayRequestDetail(IDType = "CBC", IDNumber = "1234567890")

  val requestCommonForDisplay: RequestCommonForDisplay =
    RequestCommonForDisplay(
      regime = "CBC",
      conversationID = Some("bffaa447-b500-49e0-9c73-bfd81db9242f"),
      receiptDate = "2020-09-23T16:12:11Z",
      acknowledgementReference = "Abc12345",
      originatingSystem = "MDTP",
      requestParameters = None
    )

  val displaySubscriptionForCBCRequest: DisplaySubscriptionForCBCRequest =
    DisplaySubscriptionForCBCRequest(
      DisplaySubscriptionDetails(requestCommon = requestCommonForDisplay, requestDetail = displayRequestDetail)
    )

  "DisplaySubscriptionForCBCRequest" should {

    "deserialize DisplaySubscriptionForCBCRequest" in {
      val jsonPayload: String =
        s"""
           |{
           |  "displaySubscriptionForCBCRequest": {
           |    "requestCommon": {
           |      "regime": "CBC",
           |      "conversationID": "bffaa447-b500-49e0-9c73-bfd81db9242f",
           |      "receiptDate": "2020-09-23T16:12:11Z",
           |      "acknowledgementReference": "Abc12345",
           |      "originatingSystem": "MDTP"
           |    },
           |    "requestDetail": {
           |      "IDType": "CBC",
           |      "IDNumber": "1234567890"
           |    }
           |  }
           |}""".stripMargin

      Json.parse(jsonPayload).validate[DisplaySubscriptionForCBCRequest].get mustBe displaySubscriptionForCBCRequest
    }

    "serialise DisplaySubscriptionForCBCRequest" in {
      val json: JsObject = Json.obj(
        "displaySubscriptionForCBCRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "regime"                   -> "CBC",
            "conversationID"           -> "bffaa447-b500-49e0-9c73-bfd81db9242f",
            "receiptDate"              -> "2020-09-23T16:12:11Z",
            "acknowledgementReference" -> "Abc12345",
            "originatingSystem"        -> "MDTP"
          ),
          "requestDetail" -> Json.obj(
            "IDType"   -> "CBC",
            "IDNumber" -> "1234567890"
          )
        )
      )

      Json.toJson(displaySubscriptionForCBCRequest) mustBe json
    }

    "generate a correct request common" in {
      val requestCommon = RequestCommonForDisplay()
      val ackRefLength = requestCommon.acknowledgementReference.length
      ackRefLength >= 1 && ackRefLength <= 32 mustBe true

      requestCommon.regime mustBe "CBC"

      val date: Regex = raw"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z".r
      date.findAllIn(requestCommon.receiptDate).toList.nonEmpty mustBe true

      requestCommon.originatingSystem mustBe "MDTP"
    }
  }
}
