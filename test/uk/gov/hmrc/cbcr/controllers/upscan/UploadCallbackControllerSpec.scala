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

package uk.gov.hmrc.cbcr.controllers.upscan

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.cbcr.models.upscan.CallbackBody
import uk.gov.hmrc.cbcr.services.UpscanCallbackDispatcher
import uk.gov.hmrc.cbcr.util.SpecBase

import scala.concurrent.Future

class UploadCallbackControllerSpec extends SpecBase {
  val mockUpscanCallbackDispatcher: UpscanCallbackDispatcher = mock[UpscanCallbackDispatcher]

  val application: GuiceApplicationBuilder = applicationBuilder()
    .overrides(
      bind[UpscanCallbackDispatcher].toInstance(mockUpscanCallbackDispatcher)
    )

  "UploadCallback Controller" must {
    "accept a callback and send to the dispatcher and return an ok" in {
      val controller = application.injector.instanceOf[UploadCallbackController]

      when(mockUpscanCallbackDispatcher.handleCallback(any[CallbackBody]()))
        .thenReturn(Future.successful(true))

      val json =
        """
          |{
          |   "fileStatus": "READY",
          |   "reference": "ref",
          |   "downloadUrl": "http://test.com",
          |   "uploadDetails": {
          |                 "uploadTimestamp": 1591464117,
          |                 "checksum": "",
          |                 "fileMimeType": "",
          |                 "fileName": ""
          |               }
          |}""".stripMargin

      val result: Future[Result] = controller.callback(FakeRequest("POST", "/", FakeHeaders(), Json.parse(json)))

      status(result) shouldBe OK
      verify(mockUpscanCallbackDispatcher, times(1)).handleCallback(any[CallbackBody]())
    }
  }

}
