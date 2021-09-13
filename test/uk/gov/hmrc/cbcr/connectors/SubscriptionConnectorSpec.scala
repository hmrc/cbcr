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

package uk.gov.hmrc.cbcr.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.MustMatchers.convertToAnyMustWrapper
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{BAD_REQUEST, CONFLICT, FORBIDDEN, INTERNAL_SERVER_ERROR, METHOD_NOT_ALLOWED, NOT_FOUND, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.cbcr.generators.ModelGenerators
import uk.gov.hmrc.cbcr.util.SpecBase
import uk.gov.hmrc.cbcr.helpers.WireMockServerHandler
import uk.gov.hmrc.cbcr.models.subscription.request.{DisplaySubscriptionForCBCRequest, UpdateSubscriptionForCBCRequest}

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionConnectorSpec
    extends SpecBase with WireMockServerHandler with ModelGenerators with ScalaCheckPropertyChecks {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.registration.port" -> server.port()
    )
    .build()

  lazy val connector: SubscriptionConnector = app.injector.instanceOf[SubscriptionConnector]

  val errorStatusCodes: Seq[Int] =
    Seq(BAD_REQUEST, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, CONFLICT, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE)

  "SubscriptionConnector" should {

    "displaySubscriptionForCBC" should {
      "return status OK for a successful request to display subscription" in {

        forAll(arbitrary[DisplaySubscriptionForCBCRequest]) { displaySubscriptionForCBCRequest =>
          stubResponse("/country-by-country-stubs/dac6/dct04/v1", OK)

          val result = connector.displaySubscriptionForCBC(displaySubscriptionForCBCRequest)
          result.futureValue.status mustBe OK
        }
      }

      "return status non-OK for an unsuccessful request to display subscription" in {

        forAll(arbitrary[DisplaySubscriptionForCBCRequest], Gen.oneOf(errorStatusCodes)) {
          (displaySubscriptionForCBCRequest, statusCode) =>
            stubResponse("/country-by-country-stubs/dac6/dct04/v1", statusCode)

            val result = connector.displaySubscriptionForCBC(displaySubscriptionForCBCRequest)
            result.futureValue.status mustBe statusCode
        }
      }
    }
  }

  private def stubResponse(expectedUrl: String, expectedStatus: Int): StubMapping =
    server.stubFor(
      post(urlEqualTo(expectedUrl))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
        )
    )

}
