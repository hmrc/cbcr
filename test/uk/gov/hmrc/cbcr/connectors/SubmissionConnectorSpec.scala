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
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.cbcr.util.SpecBase
import uk.gov.hmrc.cbcr.helpers.WireMockHelper

class SubmissionConnectorSpec extends SpecBase with WireMockHelper {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.submission.port" -> server.port()
      )
      .build()

  lazy val connector: SubmissionConnector = app.injector.instanceOf[SubmissionConnector]

  //TODO - DAC6-1015 - Change below submission URLs when URL is provided

  "Submission Connector" should {
    "should return OK when the backend returns a valid successful response" in {

      server.stubFor(
        post(urlEqualTo("/cbcr-stubs/dac6/dct06/v1"))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      val xml = <test></test>

      whenReady(connector.submitReport(xml)) { result =>
        result.status shouldBe OK
      }
    }

    "throw an exception when upscan returns a 4xx response" in {

      server.stubFor(
        post(urlEqualTo("/cbcr-stubs/dac6/dct06/v1"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
          )
      )

      val xml = <test></test>

      val result = connector.submitReport(xml)

      result.futureValue.status shouldBe BAD_REQUEST
    }

    "throw an exception when upscan returns 5xx response" in {

      server.stubFor(
        post(urlEqualTo("/cbcr-stubs/dac6/dct06/v1"))
          .willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
          )
      )

      val xml = <test></test>
      val result = connector.submitReport(xml)
      result.futureValue.status shouldBe SERVICE_UNAVAILABLE
    }
  }
}
