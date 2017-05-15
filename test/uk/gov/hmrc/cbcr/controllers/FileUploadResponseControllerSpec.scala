/*
 * Copyright 2017 HM Revenue & Customs
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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Helpers}
import reactivemongo.api.commands.{DefaultWriteResult, WriteError}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.FileUploadRepository
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

/**
  * Created by max on 03/04/17.
  */
class FileUploadResponseControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val fir = UploadFileResponse("id1", "fid1", "status")

  val okResult = DefaultWriteResult(true, 0, Seq.empty, None, None, None)

  val failResult = DefaultWriteResult(false, 1, Seq(WriteError(1, 1, "Error")), None, None, Some("Error"))

  val fakePostRequest: FakeRequest[JsValue] = FakeRequest(Helpers.POST, "/saveFileUploadResponse").withBody(toJson(fir))

  val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(Helpers.GET, "/retrieveFileUploadResponse")

  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()

  val repo = mock[FileUploadRepository]

  val controller = new FileUploadResponseController(repo)

  "The FileUploadResponseController" should {
    "respond with a 200 when asked to store an UploadFileResponse" in {
      when(repo.save(any(classOf[UploadFileResponse]))).thenReturn(Future.successful(okResult))
      val result     = controller.saveFileUploadResponse(fakePostRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 500 if there is a DB failure" in {
      when(repo.save(any(classOf[UploadFileResponse]))).thenReturn(Future.successful(failResult))
      val result = controller.saveFileUploadResponse(fakePostRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "respond with a 200 and a FileUploadResponse when asked to retrieve an existing envelopeId" in {
      when(repo.get(any(classOf[String]))).thenReturn(Future.successful(Some(fir)))
      val result = controller.retrieveFileUploadResponse("envelopeIdOk")(fakeGetRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(result).validate[UploadFileResponse].isSuccess shouldBe true
    }

    "respond with a 404 when asked to retrieve a non-existent envelopeId" in {
      when(repo.get(any(classOf[String]))).thenReturn(Future.successful(None))
      val result = controller.retrieveFileUploadResponse("envelopeIdFail")(fakeGetRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

  }

}
