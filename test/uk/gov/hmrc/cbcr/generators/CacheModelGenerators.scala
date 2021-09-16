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
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.cbcr.models.subscription._
import uk.gov.hmrc.cbcr.models.subscription.request._

trait CacheModelGenerators extends BaseGenerators with JavaTimeGenerators {

  implicit val arbitrarySubscriptionForDACRequest: Arbitrary[SubscriptionForCBCRequest] = Arbitrary {
    for {
      requestCommon <- arbitrary[RequestCommonForSubscription]
      requestDetail <- arbitrary[CreateRequestDetail]
    } yield SubscriptionForCBCRequest(requestCommon, requestDetail)
  }

  implicit val arbitraryCreateSubscriptionForDACRequest: Arbitrary[CreateSubscriptionForCBCRequest] = Arbitrary {
    for {
      subscription   <- arbitrary[SubscriptionForCBCRequest]
      subscriptionID <- arbitrary[String]
    } yield CreateSubscriptionForCBCRequest(subscription, subscriptionID)
  }

  implicit val arbitraryRequestDetail: Arbitrary[CreateRequestDetail] = Arbitrary {
    for {
      idType           <- arbitrary[String]
      idNumber         <- arbitrary[String]
      tradingName      <- arbitrary[Option[String]]
      isGBUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[PrimaryContact]
      secondaryContact <- arbitrary[Option[SecondaryContact]]
    } yield CreateRequestDetail(idType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryOrganisationDetails: Arbitrary[OrganisationDetails] = Arbitrary {
    for {
      organisationName <- arbitrary[String]
    } yield OrganisationDetails(organisationName)
  }

  implicit val arbitraryContactInformationForOrganisation: Arbitrary[ContactInformationForOrganisation] = Arbitrary {
    for {
      organisation <- arbitrary[OrganisationDetails]
      email        <- arbitrary[String]
      phone        <- arbitrary[Option[String]]
      mobile       <- arbitrary[Option[String]]
    } yield ContactInformationForOrganisation(organisation, email, phone, mobile)
  }

  implicit val arbitraryPrimaryContact: Arbitrary[PrimaryContact] = Arbitrary {
    for {
      contactInformation <- arbitrary[ContactInformationForOrganisation]
    } yield PrimaryContact(contactInformation)
  }

  implicit val arbitrarySecondaryContact: Arbitrary[SecondaryContact] = Arbitrary {
    for {
      contactInformation <- arbitrary[ContactInformationForOrganisation]
    } yield SecondaryContact(contactInformation)
  }

  implicit val arbitraryRequestParameter: Arbitrary[RequestParameter] = Arbitrary {
    for {
      paramName  <- arbitrary[String]
      paramValue <- arbitrary[String]
    } yield RequestParameter(paramName, paramValue)
  }

  implicit val arbitraryRequestCommonForSubscription: Arbitrary[RequestCommonForSubscription] = Arbitrary {
    for {
      regime                   <- nonEmptyString
      receiptDate              <- nonEmptyString
      acknowledgementReference <- nonEmptyString
      originatingSystem        <- nonEmptyString
      requestParameters        <- arbitrary[Option[Seq[RequestParameter]]]
    } yield
      RequestCommonForSubscription(regime, receiptDate, acknowledgementReference, originatingSystem, requestParameters)
  }

  implicit lazy val arbitraryDisplaySubscriptionForDACRequest: Arbitrary[DisplaySubscriptionForCBCRequest] = {
    Arbitrary {
      for {
        idNumber                 <- stringsWithMaxLength(30)
        conversationID           <- Gen.option(stringsWithMaxLength(36))
        receiptDate              <- nonEmptyString
        acknowledgementReference <- nonEmptyString
        originatingSystem        <- nonEmptyString
      } yield {
        DisplaySubscriptionForCBCRequest(
          DisplaySubscriptionDetails(
            requestCommon = RequestCommonForDisplay(
              regime = "DAC",
              conversationID = conversationID,
              receiptDate = receiptDate,
              acknowledgementReference = acknowledgementReference,
              originatingSystem = originatingSystem,
              requestParameters = None
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
}
