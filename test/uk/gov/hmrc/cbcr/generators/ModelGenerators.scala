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

package uk.gov.hmrc.cbcr.generators

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.{alphaStr, choose, listOfN}
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cbcr.models.subscription._
import uk.gov.hmrc.cbcr.models.subscription.request._

trait ModelGenerators extends BaseGenerators with JavaTimeGenerators {

  implicit val arbitraryReturnParameters: Arbitrary[RequestParameter] = Arbitrary {
    for {
      paramName  <- alphaStr
      paramValue <- alphaStr
    } yield RequestParameter(paramName, paramValue)
  }

  implicit lazy val arbitraryDisplaySubscriptionForCBCRequest: Arbitrary[DisplaySubscriptionForCBCRequest] = {
    Arbitrary {
      for {
        idNumber                 <- stringsWithMaxLength(30)
        conversationID           <- Gen.option(stringsWithMaxLength(36))
        receiptDate              <- arbitrary[String]
        acknowledgementReference <- arbitrary[String]
        originatingSystem        <- arbitrary[String]
        requestParameter         <- Gen.option(Gen.listOf(arbitrary[RequestParameter]))
      } yield {
        DisplaySubscriptionForCBCRequest(
          DisplaySubscriptionDetails(
            requestCommon = RequestCommonForDisplay(
              regime = "DAC",
              conversationID = conversationID,
              receiptDate = receiptDate,
              acknowledgementReference = acknowledgementReference,
              originatingSystem = originatingSystem,
              requestParameters = requestParameter
            ),
            requestDetail = DisplayRequestDetail(
              IDType = "DAC",
              IDNumber = idNumber
            )
          )
        )
      }
    }
  }

  implicit val arbitraryContactInformationForOrganisation: Arbitrary[ContactInformationForOrganisation] = Arbitrary {
    for {
      name      <- nonEmptyString
      email     <- nonEmptyString
      telephone <- Gen.option(nonEmptyString)
      mobile    <- Gen.option(nonEmptyString)
    } yield ContactInformationForOrganisation(OrganisationDetails(name), email, telephone, mobile)
  }

  implicit val arbitrarySecondaryContact: Arbitrary[SecondaryContact] = Arbitrary {
    for {
      contactInformation <- arbitrary[ContactInformationForOrganisation]
    } yield SecondaryContact(contactInformation)
  }

  implicit lazy val arbitraryUpdateSubscriptionForDACRequest: Arbitrary[UpdateSubscriptionForCBCRequest] = {
    Arbitrary {
      for {
        idNumber                 <- stringsWithMaxLength(30)
        receiptDate              <- nonEmptyString
        acknowledgementReference <- nonEmptyString
        originatingSystem        <- nonEmptyString
        requestParameter         <- Gen.option(Gen.listOf(arbitrary[RequestParameter]))
        isGBUser                 <- arbitrary[Boolean]
        primaryContact           <- arbitrary[ContactInformationForOrganisation]
        secondaryContact         <- Gen.option(arbitrary[SecondaryContact])
      } yield {
        UpdateSubscriptionForCBCRequest(
          UpdateSubscriptionDetails(
            requestCommon = RequestCommonForUpdate(
              regime = "CBC",
              receiptDate = receiptDate,
              acknowledgementReference = acknowledgementReference,
              originatingSystem = originatingSystem,
              requestParameters = requestParameter
            ),
            requestDetail = RequestDetailForUpdate(
              IDType = "SAFE",
              IDNumber = idNumber,
              tradingName = None,
              isGBUser = isGBUser,
              primaryContact = PrimaryContact(primaryContact),
              secondaryContact = secondaryContact
            )
          )
        )
      }
    }
  }

}
