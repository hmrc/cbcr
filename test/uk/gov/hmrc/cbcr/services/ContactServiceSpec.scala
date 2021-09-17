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

package uk.gov.hmrc.cbcr.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.cbcr.connectors.SubscriptionConnector
import uk.gov.hmrc.cbcr.models.JsonFixtures.contactsResponse
import uk.gov.hmrc.cbcr.models.subscription.response.{DisplaySubscriptionForCBCResponse, ResponseCommon, ResponseDetail, SubscriptionForCBCResponse}
import uk.gov.hmrc.cbcr.models.subscription.{ContactInformationForOrganisation, OrganisationDetails, PrimaryContact, SecondaryContact, SubscriptionDetails}
import uk.gov.hmrc.cbcr.util.SpecBase
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContactServiceSpec extends SpecBase with BeforeAndAfterEach {

  override def beforeEach(): Unit = reset(mockSubscriptionConnector, mockSubscriptionCacheService)

  val mockSubscriptionConnector = mock[SubscriptionConnector]
  val mockSubscriptionCacheService = mock[SubscriptionCacheService]

  "Contact Service Spec" should {
    val application = applicationBuilder()
      .overrides(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
        bind[SubscriptionCacheService].toInstance(mockSubscriptionCacheService)
      )
      .build()

    "must correctly retrieve subscription from connector when not present in cache" in {
      val service = application.injector.instanceOf[ContactService]

      when(mockSubscriptionConnector.displaySubscriptionForCBC(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, contactsResponse)))

      when(mockSubscriptionCacheService.retrieveSubscriptionDetails(any())(any()))
        .thenReturn(Future.successful(None))

      val expectedSubscriptionDetails =
        SubscriptionDetails(
          "111111111",
          Some("tradingName"),
          true,
          ContactInformationForOrganisation(OrganisationDetails("name"), "test@g.com", Some("phone"), Some("mobile")),
          Some(ContactInformationForOrganisation(OrganisationDetails("orgName"), "email@n.com", None, None))
        )

      implicit val userRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val result = service.getLatestContacts("111111111")

      whenReady(result) { sub =>
        sub mustBe expectedSubscriptionDetails
        verify(mockSubscriptionCacheService, times(1)).retrieveSubscriptionDetails(any())(any())
        verify(mockSubscriptionConnector, times(1)).displaySubscriptionForCBC(any())(any(), any())
      }
    }

    "must correctly retrieve subscription when present in cache" in {
      val service = application.injector.instanceOf[ContactService]

      when(mockSubscriptionConnector.displaySubscriptionForCBC(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, contactsResponse)))

      val g = DisplaySubscriptionForCBCResponse(
        SubscriptionForCBCResponse(
          ResponseCommon("", None, "", None),
          ResponseDetail(
            "111111111",
            Some(""),
            true,
            PrimaryContact(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None)),
            Some(SecondaryContact(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None)))
          )
        )
      )

      when(mockSubscriptionCacheService.retrieveSubscriptionDetails(any())(any()))
        .thenReturn(
          Future.successful(
            Some(
              DisplaySubscriptionForCBCResponse(
                SubscriptionForCBCResponse(
                  ResponseCommon("", None, "", None),
                  ResponseDetail(
                    "111111111",
                    Some(""),
                    true,
                    PrimaryContact(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None)),
                    Some(SecondaryContact(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None)))
                  )
                )
              )
            )
          )
        )

      val subscriptionDetails = SubscriptionDetails(
        "111111111",
        Some(""),
        true,
        ContactInformationForOrganisation(OrganisationDetails(""), "", None, None),
        Some(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None))
      )

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val result = service.getLatestContacts("111111111")

      whenReady(result) { sub =>
        sub mustBe subscriptionDetails
        verify(mockSubscriptionCacheService, times(1)).retrieveSubscriptionDetails(any())(any())
        verify(mockSubscriptionConnector, times(0)).displaySubscriptionForCBC(any())(any(), any())
      }
    }
  }
}
