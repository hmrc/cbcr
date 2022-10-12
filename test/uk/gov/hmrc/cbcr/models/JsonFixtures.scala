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

import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}
import uk.gov.hmrc.cbcr.models.subscription.response.{DisplaySubscriptionForCBCResponse, ResponseCommon, ResponseDetail, SubscriptionForCBCResponse}
import uk.gov.hmrc.cbcr.models.subscription.{ContactInformationForOrganisation, OrganisationDetails, PrimaryContact, SecondaryContact}

object JsonFixtures {

  val contactsModel =
    DisplaySubscriptionForCBCResponse(
      SubscriptionForCBCResponse(
        ResponseCommon("OK", None, "2020-08-09T11:23:45Z", None),
        ResponseDetail(
          "111111111",
          Some(""),
          true,
          PrimaryContact(ContactInformationForOrganisation(OrganisationDetails("name"), "", Some(""), Some(""))),
          Some(SecondaryContact(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None)))
        )
      )
    )

  val contactsResponse =
    """
      |{
      |"displaySubscriptionForCBCResponse": {
      |"responseCommon": {
      |"status": "OK",
      |"processingDate": "2020-08-09T11:23:45Z"
      |},
      |"responseDetail": {
      |"subscriptionID": "111111111",
      |"tradingName": "tradingName",
      |"isGBUser": true,
      |"primaryContact": [
      |{
      |"email": "test@g.com",
      |"phone": "phone",
      |"mobile": "mobile",
      |"organisation": {
      |"organisationName": "name"
      |}
      |}
      |],
      |"secondaryContact": [
      |{
      |"email": "email@n.com",
      |"organisation": {
      |"organisationName": "orgName"
      |}
      |}
      |]
      |}
      |}
      |}""".stripMargin

  def updateDetailsPayloadNoSecondContact(
    idNumber: JsString,
    isGBUser: JsBoolean,
    orgName: JsString,
    primaryEmail: JsString): String =
    s"""
       |{
       |  "updateSubscriptionForCBCRequest": {
       |    "requestCommon": {
       |      "regime": "CBC",
       |      "receiptDate": "2020-09-23T16:12:11Z",
       |      "acknowledgementReference": "AB123c",
       |      "originatingSystem": "MDTP",
       |      "requestParameters": [{
       |        "paramName":"Name",
       |        "paramValue":"Value"
       |      }]
       |    },
       |    "requestDetail": {
       |      "IDType": "SAFE",
       |      "IDNumber": $idNumber,
       |      "isGBUser": $isGBUser,
       |      "primaryContact": [{
       |        "organisation": {
       |          "organisationName": $orgName
       |        },
       |        "email": $primaryEmail
       |      }]
       |    }
       |  }
       |}
       |""".stripMargin

  def updateDetailsPayload(
    idNumber: JsString,
    isGBUser: JsBoolean,
    orgName: JsString,
    email: JsString,
    phone: JsString): String =
    s"""
       |{
       |  "updateSubscriptionForCBCRequest": {
       |    "requestCommon": {
       |      "regime": "CBC",
       |      "receiptDate": "2020-09-23T16:12:11Z",
       |      "acknowledgementReference": "AB123c",
       |      "originatingSystem": "MDTP",
       |      "requestParameters": [{
       |        "paramName":"Name",
       |        "paramValue":"Value"
       |      }]
       |    },
       |    "requestDetail": {
       |      "IDType": "SAFE",
       |      "IDNumber": $idNumber,
       |      "isGBUser": $isGBUser,
       |      "primaryContact": [{
       |        "organisation": {
       |          "organisationName": $orgName
       |        },
       |        "email": $email
       |      }],
       |      "secondaryContact": [{
       |        "organisation": {
       |          "organisationName": $orgName
       |        },
       |        "email": $email,
       |        "phone": $phone
       |      }]
       |    }
       |  }
       |}
       |""".stripMargin

  def updateDetailsJsonNoSecondContact(
    idNumber: String,
    isGBUser: Boolean,
    orgName: String,
    primaryEmail: String): JsObject =
    Json.obj(
      "updateSubscriptionForCBCRequest" -> Json.obj(
        "requestCommon" -> Json.obj(
          "regime"                   -> "CBC",
          "receiptDate"              -> "2020-09-23T16:12:11Z",
          "acknowledgementReference" -> "AB123c",
          "originatingSystem"        -> "MDTP",
          "requestParameters" -> Json.arr(
            Json.obj(
              "paramName"  -> "Name",
              "paramValue" -> "Value"
            )
          )
        ),
        "requestDetail" -> Json.obj(
          "IDType"   -> "SAFE",
          "IDNumber" -> idNumber,
          "isGBUser" -> isGBUser,
          "primaryContact" ->
            Json.obj(
              "organisation" -> Json.obj(
                "organisationName" -> orgName,
              ),
              "email" -> primaryEmail
            )
        )
      )
    )

  def updateDetailsJson(idNumber: String, isGBUser: Boolean, email: String, orgName: String, phone: String): JsObject =
    Json.obj(
      "updateSubscriptionForCBCRequest" -> Json.obj(
        "requestCommon" -> Json.obj(
          "regime"                   -> "CBC",
          "receiptDate"              -> "2020-09-23T16:12:11Z",
          "acknowledgementReference" -> "AB123c",
          "originatingSystem"        -> "MDTP",
          "requestParameters" -> Json.arr(
            Json.obj(
              "paramName"  -> "Name",
              "paramValue" -> "Value"
            )
          )
        ),
        "requestDetail" -> Json.obj(
          "IDType"   -> "SAFE",
          "IDNumber" -> idNumber,
          "isGBUser" -> isGBUser,
          "primaryContact" ->
            Json.obj(
              "organisation" -> Json.obj(
                "organisationName" -> orgName
              ),
              "email" -> email
            ),
          "secondaryContact" ->
            Json.obj(
              "organisation" -> Json.obj(
                "organisationName" -> orgName
              ),
              "email" -> email,
              "phone" -> phone
            )
        )
      )
    )

}
