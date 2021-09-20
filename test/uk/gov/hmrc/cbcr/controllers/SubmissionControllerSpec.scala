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
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.connectors.SubmissionConnector
import uk.gov.hmrc.cbcr.models.subscription.{ContactInformationForOrganisation, OrganisationDetails, SubscriptionDetails}
import uk.gov.hmrc.cbcr.services.ContactService
import uk.gov.hmrc.cbcr.util.SpecBase
import uk.gov.hmrc.cbcr.util.SubmissionFixtures.minimalPassing
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase with BeforeAndAfterEach with MockAuth {

  val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]
  val mockContactService: ContactService = mock[ContactService]

  val application: Application =
    applicationBuilder()
      .overrides(
        bind[SubmissionConnector].toInstance(mockSubmissionConnector),
        bind[ContactService].toInstance(mockContactService),
        bind[CBCRAuth].toInstance(cBCRAuth)
      )
      .build()

  val errorStatusCodes: Seq[Int] =
    Seq(BAD_REQUEST, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, CONFLICT, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE)

  override def beforeEach(): Unit = reset(mockSubmissionConnector)

  "submitReport" should {
    "return ok with status" in {
      val details = SubscriptionDetails(
        "111111111",
        Some("tradingName"),
        true,
        ContactInformationForOrganisation(OrganisationDetails("org1"), "test@email.com", None, None),
        Some(ContactInformationForOrganisation(OrganisationDetails("org2"), "test1@email.com", None, None))
      )

      when(mockSubmissionConnector.submitReport(any())(any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))
      when(mockContactService.getLatestContacts(any())(any(), any(), any()))
        .thenReturn(Future.successful(details))

      val request = FakeRequest(POST, routes.SubmissionController.submitDocument.url).withXmlBody(minimalPassing)
      val result: Future[Result] = route(application, request).value

      status(result) shouldBe OK
    }

    "must return errorCode when none is returned" in {

      val statusCode = Gen.oneOf(errorStatusCodes).sample.value

      when(mockSubmissionConnector.submitReport(any())(any()))
        .thenReturn(Future.successful(HttpResponse(statusCode, "")))

      val request = FakeRequest(POST, routes.SubmissionController.submitDocument.url).withXmlBody(minimalPassing)

      val result = route(application, request).value

      status(result) shouldBe statusCode
    }
  }
}
