/*
 * Copyright 2023 HM Revenue & Customs
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

import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.FileUploadRepository
import uk.gov.hmrc.cbcr.util.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by max on 03/04/17.
  */
class FileUploadResponseControllerSpec extends UnitSpec with ScalaFutures with MockAuth {

  private val fur = FileUploadResponse("id1", "fid1", "AVAILABLE", None)

  val fakePostRequest: FakeRequest[FileUploadResponse] =
    FakeRequest(Helpers.POST, "/saveFileUploadResponse").withBody(fur)

  val badFakePostRequest: FakeRequest[JsValue] =
    FakeRequest(Helpers.POST, "/saveFileUploadResponse")
      .withHeaders("Content-Type" -> "application/json")
      .withBody(Json.obj("bad" -> "request"))

  val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(Helpers.GET, "/retrieveFileUploadResponse")

  implicit val as: ActorSystem = ActorSystem()

  private val repo = mock[FileUploadRepository]

  val controller = new FileUploadResponseController(repo, auth, cc)

  "The FileUploadResponseController" should {
    "respond with 200 and don't store when received status other than AVAILABLE or DELETED" in {
      val result = controller.saveFileUploadResponse(fakePostRequest.withBody(fur.copy(status = "QUARANTINED")))
      status(result) shouldBe Status.OK
    }

    "respond with a 200 when asked to store an FileUploadResponse" in {
      when(repo.save2(any(classOf[FileUploadResponse]))).thenReturn(Future.successful(None))
      val result = controller.saveFileUploadResponse(fakePostRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 400 if FileUploadResponse in request is invalid" in {
      when(repo.save2(any(classOf[FileUploadResponse])))
        .thenReturn(Future.failed(new RuntimeException()))
      val result = controller.saveFileUploadResponse(badFakePostRequest).run()
      status(result) shouldBe Status.BAD_REQUEST
    }

    "respond with a 200 and a FileUploadResponse when asked to retrieve an existing envelopeId" in {
      when(repo.get(any(classOf[String]))).thenReturn(Future.successful(Some(fur)))
      val result = controller.retrieveFileUploadResponse("envelopeIdOk")(fakeGetRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(result).validate[FileUploadResponse].isSuccess shouldBe true
    }

    "respond with a 204 when asked to retrieve a non-existent envelopeId" in {
      when(repo.get(any(classOf[String]))).thenReturn(Future.successful(None))
      val result = controller.retrieveFileUploadResponse("envelopeIdFail")(fakeGetRequest)
      status(result) shouldBe Status.NO_CONTENT
    }

  }

}
