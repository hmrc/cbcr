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

import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.cbcr.models.upscan._

import java.time.Instant
import scala.concurrent.Future

class UpscanCallbackDispatcherSpec
    extends FreeSpec with MockitoSugar with GuiceOneAppPerSuite with ScalaFutures with Matchers {

  val mockUploadProgressTracker: UploadProgressTracker = mock[UploadProgressTracker]

  val applicationBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[UploadProgressTracker].toInstance(mockUploadProgressTracker)
      )

  "UpscanCallbackDispatcher" - {

    "handleCallback must return UploadedSuccessfully for the input ReadyCallbackBody" in {
      val reference = Reference("ref")
      val uploadDetails = UploadDetails(Instant.now(), "1234", "application/xml", "test.xml", 1000)

      val readyCallbackBody = ReadyCallbackBody(
        reference,
        "downloadUrl",
        UploadDetails(Instant.now(), "1234", "application/xml", "test.xml", 1000))

      val uploadStatus = UploadedSuccessfully(
        uploadDetails.fileName,
        uploadDetails.fileMimeType,
        readyCallbackBody.downloadUrl,
        Some(uploadDetails.size))

      when(mockUploadProgressTracker.registerUploadResult(reference, uploadStatus)).thenReturn(Future.successful(true))

      val uploadCallbackDispatcher = new UpscanCallbackDispatcher(mockUploadProgressTracker)

      val result: Future[Boolean] = uploadCallbackDispatcher.handleCallback(readyCallbackBody)
      result.futureValue shouldBe true

    }

    "handleCallback must return Quarantined for the input FailedCallbackBody" in {
      val reference = Reference("ref")
      val errorDetails = ErrorDetails("QUARANTINE", "message")

      val readyCallbackBody = FailedCallbackBody(reference, errorDetails)

      val uploadStatus = Quarantined

      when(mockUploadProgressTracker.registerUploadResult(reference, uploadStatus)).thenReturn(Future.successful(true))

      val uploadCallbackDispatcher = new UpscanCallbackDispatcher(mockUploadProgressTracker)

      val result: Future[Boolean] = uploadCallbackDispatcher.handleCallback(readyCallbackBody)
      result.futureValue shouldBe true

    }

    "handleCallback must return REJECTED for the input FailedCallbackBody" in {
      val reference = Reference("ref")
      val errorDetails = ErrorDetails("REJECTED", "message")

      val readyCallbackBody = FailedCallbackBody(reference, errorDetails)

      val uploadStatus = UploadRejected(readyCallbackBody.failureDetails)

      when(mockUploadProgressTracker.registerUploadResult(reference, uploadStatus)).thenReturn(Future.successful(true))

      val uploadCallbackDispatcher = new UpscanCallbackDispatcher(mockUploadProgressTracker)

      val result: Future[Boolean] = uploadCallbackDispatcher.handleCallback(readyCallbackBody)
      result.futureValue shouldBe true

    }

    "handleCallback must return Failed for the input FailedCallbackBody" in {
      val reference = Reference("ref")
      val errorDetails = ErrorDetails("Failed", "message")

      val readyCallbackBody = FailedCallbackBody(reference, errorDetails)

      val uploadStatus = Failed

      when(mockUploadProgressTracker.registerUploadResult(reference, uploadStatus)).thenReturn(Future.successful(true))

      val uploadCallbackDispatcher = new UpscanCallbackDispatcher(mockUploadProgressTracker)

      val result: Future[Boolean] = uploadCallbackDispatcher.handleCallback(readyCallbackBody)
      result.futureValue shouldBe true

    }

  }
}