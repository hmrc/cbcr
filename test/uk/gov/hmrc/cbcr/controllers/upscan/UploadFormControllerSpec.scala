/*
 * Copyright 2022 HM Revenue & Customs
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

import org.bson.types.ObjectId
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, status, _}
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.upscan._
import uk.gov.hmrc.cbcr.repositories.UploadSessionRepository
import uk.gov.hmrc.cbcr.services.UploadProgressTracker
import uk.gov.hmrc.cbcr.util.SpecBase

import scala.concurrent.Future

class UploadFormControllerSpec extends SpecBase with BeforeAndAfterEach with MockAuth {

  val mockUploadSessionRepository: UploadSessionRepository = mock[UploadSessionRepository]
  val mockUploadProgressTracker: UploadProgressTracker = mock[UploadProgressTracker]

  val application: Application =
    applicationBuilder()
      .overrides(
        bind[UploadProgressTracker].toInstance(mockUploadProgressTracker),
        bind[UploadSessionRepository].toInstance(mockUploadSessionRepository),
        bind[CBCRAuth].toInstance(cBCRAuth)
      )
      .build()

  "getDetails" should {
    "must return ok with status" in {

      val uploadDetails = UploadSessionDetails(
        ObjectId.get(),
        UploadId("123"),
        Reference("123"),
        InProgress
      )

      when(mockUploadSessionRepository.findByUploadId(UploadId("uploadID")))
        .thenReturn(Future.successful(Some(uploadDetails)))

      val request = FakeRequest(GET, routes.UploadFormController.getDetails("uploadID").url)

      val result = route(application, request).value

      status(result) mustEqual OK
    }

    "must return 404 when none is returned" in {

      when(mockUploadSessionRepository.findByUploadId(UploadId("uploadID")))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(GET, routes.UploadFormController.getDetails("uploadID").url)

      val result = route(application, request).value

      status(result) mustEqual NOT_FOUND
    }
  }

  "getStatus" should {
    "return ok with status" in {

      when(mockUploadProgressTracker.getUploadResult(UploadId("uploadID")))
        .thenReturn(Future.successful(Some(InProgress)))

      val request = FakeRequest(GET, routes.UploadFormController.getStatus("uploadID").url)

      val result = route(application, request).value

      status(result) mustEqual OK
    }

    "must return 404 when none is returned" in {

      when(mockUploadProgressTracker.getUploadResult(UploadId("uploadID"))).thenReturn(Future.successful(None))

      val request = FakeRequest(GET, routes.UploadFormController.getStatus("uploadID").url)

      val result = route(application, request).value

      status(result) mustEqual NOT_FOUND
    }
  }
}
