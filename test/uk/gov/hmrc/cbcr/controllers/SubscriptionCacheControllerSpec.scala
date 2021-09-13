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

package uk.gov.hmrc.cbcr.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.connectors.SubscriptionConnector
import uk.gov.hmrc.cbcr.generators.CacheModelGenerators
import uk.gov.hmrc.cbcr.models.subscription.request.{CreateSubscriptionForCBCRequest, DisplaySubscriptionForCBCRequest}
import uk.gov.hmrc.cbcr.models.subscription.response.{DisplaySubscriptionForCBCResponse, ResponseCommon, ResponseDetail, SubscriptionForCBCResponse}
import uk.gov.hmrc.cbcr.models.subscription.{ContactInformationForOrganisation, OrganisationDetails, PrimaryContact, SecondaryContact}
import uk.gov.hmrc.cbcr.services.SubscriptionCacheService
import uk.gov.hmrc.cbcr.util.SpecBase
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class SubscriptionCacheControllerSpec
    extends SpecBase with MockAuth with CacheModelGenerators with BeforeAndAfterEach with ScalaCheckPropertyChecks {

  val mockSubscriptionCacheService: SubscriptionCacheService = mock[SubscriptionCacheService]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  val application: Application =
    applicationBuilder()
      .overrides(
        bind[SubscriptionCacheService].toInstance(mockSubscriptionCacheService),
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
        bind[CBCRAuth].toInstance(cBCRAuth)
      )
      .build()

  override def beforeEach(): Unit = {
    reset(mockSubscriptionCacheService)
    reset(mockSubscriptionConnector)
  }

  val errorStatusCodes: Seq[Int] = Seq(
    BAD_REQUEST,
    FORBIDDEN,
    NOT_FOUND,
    METHOD_NOT_ALLOWED,
    CONFLICT,
    INTERNAL_SERVER_ERROR,
    SERVICE_UNAVAILABLE
  )

  "Cache Controller" should {
    "store a subscription when given a valid create subscription payload" in {
      when(mockSubscriptionCacheService.storeSubscriptionDetails(any(), any()))
        .thenReturn(Future.successful(true))

      forAll(arbitrary[CreateSubscriptionForCBCRequest]) { subscriptionRequest =>
        val payload = Json.toJson(subscriptionRequest)
        val request = FakeRequest(POST, routes.SubscriptionCacheController.storeSubscriptionDetails.url)
          .withJsonBody(payload)

        val result: Future[Result] = route(application, request).value

        status(result) mustBe OK

      }
    }

    "retrieve a subscription from the cache where one exists" in {
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
                    isGBUser = true,
                    PrimaryContact(
                      ContactInformationForOrganisation(OrganisationDetails("Name"), "", Some(""), Some(""))),
                    Some(SecondaryContact(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None)))
                  )
                )
              )
            )
          )
        )

      forAll(arbitrary[DisplaySubscriptionForCBCRequest]) { display =>
        val payload = Json.toJson(display)

        val request = FakeRequest(POST, routes.SubscriptionCacheController.retrieveSubscription.url)
          .withJsonBody(payload)

        val result: Future[Result] = route(application, request).value

        status(result) mustBe OK
        //verify(mockSubscriptionCacheService, times(1)).retrieveSubscriptionDetails(any())(any())
        verify(mockSubscriptionConnector, times(0)).displaySubscriptionForCBC(any())(any(), any())
      }
    }

    "retrieve a subscription from the hod where one does not exist in cache" in {

      forAll(arbitrary[DisplaySubscriptionForCBCRequest], Gen.oneOf(errorStatusCodes)) { (display, statusCodes) =>
        when(mockSubscriptionCacheService.retrieveSubscriptionDetails(any())(any()))
          .thenReturn(Future.successful(None))
        when(mockSubscriptionConnector.displaySubscriptionForCBC(any())(any(), any()))
          .thenReturn(Future.successful(HttpResponse(statusCodes, "")))

        val payload = Json.toJson(display)
        val request = FakeRequest(POST, routes.SubscriptionCacheController.retrieveSubscription.url)
          .withJsonBody(payload)

        val result: Future[Result] = route(application, request).value

        status(result) mustBe statusCodes
      }
    }

    "retrieve a subscription and convert result for ok response" in {

      forAll(arbitrary[DisplaySubscriptionForCBCRequest], OK) { (display, statusCodes) =>
        when(mockSubscriptionCacheService.retrieveSubscriptionDetails(any())(any()))
          .thenReturn(Future.successful(None))
        when(mockSubscriptionConnector.displaySubscriptionForCBC(any())(any(), any()))
          .thenReturn(Future.successful(HttpResponse(statusCodes, "")))

        val payload = Json.toJson(display)
        val request = FakeRequest(POST, routes.SubscriptionCacheController.retrieveSubscription.url)
          .withJsonBody(payload)

        val result: Future[Result] = route(application, request).value

        status(result) mustBe OK
      }
    }
  }

}
