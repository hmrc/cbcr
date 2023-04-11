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

import akka.actor.ActorSystem
import com.mongodb.client.result.InsertOneResult
import org.bson.BsonNull
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json._
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

  val fur = FileUploadResponse("id1", "fid1", "status", None)

  val okResult = InsertOneResult.acknowledged(BsonNull.VALUE)

  val fakePostRequest: FakeRequest[JsValue] = FakeRequest(Helpers.POST, "/saveFileUploadResponse").withBody(toJson(fur))

  val badFakePostRequest: FakeRequest[JsValue] =
    FakeRequest(Helpers.POST, "/saveFileUploadResponse").withBody(Json.obj("bad" -> "request"))

  val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(Helpers.GET, "/retrieveFileUploadResponse")

  implicit val as = ActorSystem()

  val repo = mock[FileUploadRepository]

  val controller = new FileUploadResponseController(repo, cBCRAuth, cc)

  "The FileUploadResponseController" should {
    "respond with a 200 when asked to store an FileUploadResponse" in {
      when(repo.save2(any(classOf[FileUploadResponse]))).thenReturn(Future.successful(None))
      val result = controller.saveFileUploadResponse(fakePostRequest)
      status(result) shouldBe Status.OK
    }

    "protect the AVAILABLE status from being overridden" in {
      val availableFur = fur.copy(status = "AVAILABLE")
      val quarantinedFur = fur.copy(status = "QUARANTINED")
      val argument: ArgumentCaptor[FileUploadResponse] = ArgumentCaptor.forClass(classOf[FileUploadResponse])
      when(repo.save2(argument.capture())).thenReturn(Future.successful(Some(availableFur)))
      val result = controller.saveFileUploadResponse(fakePostRequest.withBody(toJson(quarantinedFur)))

      status(result) shouldBe Status.OK
      argument.getAllValues should contain theSameElementsInOrderAs List(quarantinedFur, availableFur)
    }

    "don't protect the AVAILABLE status from being overridden by DELETED" in {
      val availableFur = fur.copy(status = "AVAILABLE")
      val deletedFur = fur.copy(status = "DELETED")
      val argument: ArgumentCaptor[FileUploadResponse] = ArgumentCaptor.forClass(classOf[FileUploadResponse])
      when(repo.save2(argument.capture())).thenReturn(Future.successful(Some(availableFur)))
      val result = controller.saveFileUploadResponse(fakePostRequest.withBody(toJson(deletedFur)))

      status(result) shouldBe Status.OK
      argument.getAllValues should contain theSameElementsInOrderAs List(deletedFur)
    }

    "respond with a 400 if FileUploadResponse in request is invalid" in {
      when(repo.save2(any(classOf[FileUploadResponse])))
        .thenReturn(Future.failed(new RuntimeException()))
      val result = controller.saveFileUploadResponse(badFakePostRequest)
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
