/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.auth

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.mvc.Results.{Ok, Unauthorized}
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, MissingBearerToken}
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.cbcr.util.UnitSpec
import play.api.test.Helpers.stubControllerComponents
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CBCRAuthSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val cc = stubControllerComponents()
  val mockMicroServiceAuthConnector = mock[AuthConnector]
  val cBCRAuth = new CBCRAuth(mockMicroServiceAuthConnector, cc)
  private type AuthAction = Request[AnyContent] => Future[Result]

  val authAction: AuthAction = { implicit request =>
    Future successful Ok
  }

  private def agentAuthStub(returnValue: Future[Option[AffinityGroup]]) =
    when(mockMicroServiceAuthConnector.authorise(any(), any[Retrieval[Option[AffinityGroup]]]())(any(), any()))
      .thenReturn(returnValue)

  val agentAffinity: Future[Option[AffinityGroup]] =
    Future successful Some(AffinityGroup.Agent)

  val organizationAffinity: Future[Option[AffinityGroup]] =
    Future successful Some(AffinityGroup.Organisation)

  val individualAffinity: Future[Option[AffinityGroup]] =
    Future successful Some(AffinityGroup.Individual)
  override def beforeEach(): Unit = reset(mockMicroServiceAuthConnector)

  "authCBCR" should {
    "return OK for an Agent" in {
      agentAuthStub(agentAffinity)

      val response: Result = await(cBCRAuth.authCBCR(authAction).apply(FakeRequest()))

      response shouldBe Ok
    }
    "return OK for an Organisation" in {
      agentAuthStub(organizationAffinity)

      val response: Result = await(cBCRAuth.authCBCR(authAction).apply(FakeRequest()))

      response shouldBe Ok
    }
    "return Unauthorized for an individual" in {
      agentAuthStub(individualAffinity)

      val response: Result = await(cBCRAuth.authCBCR(authAction).apply(FakeRequest()))

      response shouldBe Unauthorized
    }
    "return Unauthorized for any request with no bearerToken" in {
      agentAuthStub(Future.failed(MissingBearerToken("Not authorised")))

      val response: Result = await(cBCRAuth.authCBCR(authAction).apply(FakeRequest()))

      response.header.status shouldBe Status.UNAUTHORIZED

    }
  }
}
