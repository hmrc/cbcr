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

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.alphaNumStr
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsBoolean, JsString, Json}
import uk.gov.hmrc.cbcr.generators.ModelGenerators
import uk.gov.hmrc.cbcr.models.JsonFixtures.{updateDetailsJson, updateDetailsJsonNoSecondContact, updateDetailsPayload, updateDetailsPayloadNoSecondContact}
import uk.gov.hmrc.cbcr.models.subscription.request._
import uk.gov.hmrc.cbcr.util.SpecBase

class UpdateSubscriptionForCBCRequestSpec extends SpecBase with ModelGenerators with ScalaCheckPropertyChecks {

  val requestParameter = Seq(RequestParameter("Name", "Value"))

  val requestCommon: RequestCommonForUpdate = RequestCommonForUpdate(
    regime = "CBC",
    receiptDate = "2020-09-23T16:12:11Z",
    acknowledgementReference = "AB123c",
    originatingSystem = "MDTP",
    requestParameters = Some(requestParameter)
  )

  "UpdateSubscriptionForCBCRequest" should {

    "deserialize UpdateSubscriptionForCBCRequest" in {
      forAll(arbitrary[UpdateSubscriptionForCBCRequest], nonNumerics, alphaNumStr, nonNumerics) {
        (updateSubscriptionForCBC, orgName, phone, email) =>
          val requestDetail = updateSubscriptionForCBC.updateSubscriptionForCBCRequest.requestDetail

          val primaryContact: PrimaryContact = PrimaryContact(
            ContactInformationForOrganisation(OrganisationDetails(orgName), email, None, None)
          )

          val secondaryContact = SecondaryContact(
            ContactInformationForOrganisation(OrganisationDetails(orgName), email, Some(phone), None)
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = requestDetail.IDNumber,
            tradingName = None,
            isGBUser = requestDetail.isGBUser,
            primaryContact = primaryContact,
            secondaryContact = Some(secondaryContact)
          )

          val updateRequest = UpdateSubscriptionForCBCRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          val jsonPayload = updateDetailsPayload(
            JsString(requestDetail.IDNumber),
            JsBoolean(requestDetail.isGBUser),
            JsString(orgName),
            JsString(email),
            JsString(phone)
          )

          Json.parse(jsonPayload).validate[UpdateSubscriptionForCBCRequest].get mustBe updateRequest
      }
    }

    "deserialize UpdateSubscriptionForCBCRequest without secondary contact" in {
      forAll(arbitrary[UpdateSubscriptionForCBCRequest], nonNumerics, nonNumerics) {
        (updateSubscriptionForCBC, orgName, primaryEmail) =>
          val requestDetail = updateSubscriptionForCBC.updateSubscriptionForCBCRequest.requestDetail

          val primaryContactForInd: PrimaryContact = PrimaryContact(
            ContactInformationForOrganisation(OrganisationDetails(orgName), primaryEmail, None, None)
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = requestDetail.IDNumber,
            tradingName = None,
            isGBUser = requestDetail.isGBUser,
            primaryContact = primaryContactForInd,
            secondaryContact = None
          )

          val updateRequest = UpdateSubscriptionForCBCRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          val jsonPayload = updateDetailsPayloadNoSecondContact(
            JsString(requestDetail.IDNumber),
            JsBoolean(requestDetail.isGBUser),
            JsString(orgName),
            JsString(primaryEmail))

          Json.parse(jsonPayload).validate[UpdateSubscriptionForCBCRequest].get mustBe updateRequest
      }
    }

    "serialise UpdateSubscriptionForCBCRequest" in {
      forAll(arbitrary[UpdateSubscriptionForCBCRequest], nonNumerics, alphaNumStr, nonNumerics) {
        (updateSubscriptionForCBC, orgName, phone, email) =>
          val requestDetail = updateSubscriptionForCBC.updateSubscriptionForCBCRequest.requestDetail

          val primaryContactForInd: PrimaryContact = PrimaryContact(
            ContactInformationForOrganisation(OrganisationDetails(orgName), email, None, None)
          )

          val secondaryContact = SecondaryContact(
            ContactInformationForOrganisation(OrganisationDetails(orgName), email, Some(phone), None)
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = requestDetail.IDNumber,
            tradingName = None,
            isGBUser = requestDetail.isGBUser,
            primaryContact = primaryContactForInd,
            secondaryContact = Some(secondaryContact)
          )

          val updateRequest = UpdateSubscriptionForCBCRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          Json.toJson(updateRequest) mustBe updateDetailsJson(
            requestDetail.IDNumber,
            requestDetail.isGBUser,
            email,
            orgName,
            phone)
      }
    }

    "serialise UpdateSubscriptionForCBCRequest without secondary contact" in {
      forAll(arbitrary[UpdateSubscriptionForCBCRequest], nonNumerics, nonNumerics) {
        (updateSubscriptionForCBC, orgName, primaryEmail) =>
          val requestDetail = updateSubscriptionForCBC.updateSubscriptionForCBCRequest.requestDetail

          val primaryContactForInd: PrimaryContact = PrimaryContact(
            ContactInformationForOrganisation(OrganisationDetails(orgName), primaryEmail, None, None)
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = requestDetail.IDNumber,
            tradingName = None,
            isGBUser = requestDetail.isGBUser,
            primaryContact = primaryContactForInd,
            secondaryContact = None
          )

          val updateRequest = UpdateSubscriptionForCBCRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          Json.toJson(updateRequest) mustBe updateDetailsJsonNoSecondContact(
            requestDetail.IDNumber,
            requestDetail.isGBUser,
            orgName,
            primaryEmail)
      }
    }

  }

}
