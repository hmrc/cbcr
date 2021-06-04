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

import akka.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.JsString
import play.api.test.{FakeRequest, Helpers}
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.DocRefIdRepository
import uk.gov.hmrc.cbcr.services.RunMode
import uk.gov.hmrc.cbcr.util.UnitSpec

import scala.concurrent.Future

class DocRefIdControllerSpec extends UnitSpec with GuiceOneAppPerSuite with ScalaFutures with MockAuth {

  val fakePutRequest = FakeRequest(Helpers.PUT, "/DocRefId/myRefIDxx")

  val fakeDeleteRequest = FakeRequest(Helpers.DELETE, "doc-ref-id/mydocref")

  val fakeGetRequest = FakeRequest(Helpers.GET, "/DocRefId/myRefIDxx")

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val as = ActorSystem()
  val config = app.injector.instanceOf[Configuration]

  val repo = mock[DocRefIdRepository]

  val runMode = mock[RunMode]
  val controller = new DocRefIdController(repo, config, cBCRAuth, runMode, cc)

  "The DocRefIdController" should {
    "be able to save a DocRefID and" should {
      "respond with a 200 when all is good" in {
        when(repo.save(any(classOf[DocRefId]))).thenReturn(Future.successful(DocRefIdResponses.Ok))
        val result = controller.saveDocRefId(DocRefId("DocRefid"))(fakePutRequest)
        status(result) shouldBe Status.OK
      }

      "respond with a 500 if there is a DB failure" in {
        when(repo.save(any(classOf[DocRefId]))).thenReturn(Future.successful(DocRefIdResponses.Failed))
        val result = controller.saveDocRefId(DocRefId("DocRefid"))(fakePutRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "respond with a CONFLICT if that DocRefId already exists" in {
        when(repo.save(any(classOf[DocRefId]))).thenReturn(Future.successful(DocRefIdResponses.AlreadyExists))
        val result = controller.saveDocRefId(DocRefId("DocRefid"))(fakePutRequest)
        status(result) shouldBe Status.CONFLICT
      }
    }
    "be able to query a DocRefId and" should {

      "respond with a 200 when asked to query an existing and valid DocRefId" in {
        when(repo.query(any(classOf[DocRefId]))).thenReturn(Future.successful(DocRefIdResponses.Valid))
        val result = controller.query(DocRefId("DocRefId"))(fakeGetRequest)
        status(result) shouldBe Status.OK
      }

      "respond with a 404 when asked to query a non-existent DocRefId" in {
        when(repo.query(any(classOf[DocRefId]))).thenReturn(Future.successful(DocRefIdResponses.DoesNotExist))
        val result = controller.query(DocRefId("DocRefId"))(fakeGetRequest)
        status(result) shouldBe Status.NOT_FOUND
      }

      "respond with a CONFLICT when asked to query an expired DocRefId" in {
        when(repo.query(any(classOf[DocRefId]))).thenReturn(Future.successful(DocRefIdResponses.Invalid))
        val result = controller.query(DocRefId("DocRefId"))(fakeGetRequest)
        status(result) shouldBe Status.CONFLICT
      }
    }
    "be able to save a CorrRefId and DocRefId pair, and " should {
      "respond with a 200 when CorrRefId and DocRefId are both valid" in {
        when(repo.save(any(), any()))
          .thenReturn(Future.successful(DocRefIdResponses.Valid -> Some(DocRefIdResponses.Ok)))
        val result = controller.saveCorrDocRefId(CorrDocRefId(DocRefId("oldone")))(
          fakePutRequest.withJsonBody(JsString("DocRefId")))
        status(result) shouldBe Status.OK
      }
      "respond with a 404 when CorrRefId referrs to a non-existant DocRefId" in {
        when(repo.save(any(), any())).thenReturn(Future.successful(DocRefIdResponses.DoesNotExist -> None))
        val result = controller.saveCorrDocRefId(CorrDocRefId(DocRefId("oldone")))(
          fakePutRequest.withJsonBody(JsString("DocRefId")))
        status(result) shouldBe Status.NOT_FOUND
      }
      "respond with a BadRequest when the CorrRefId refers to an INVALID DocRefId" in {
        when(repo.save(any(), any())).thenReturn(Future.successful(DocRefIdResponses.Invalid -> None))
        val result = controller.saveCorrDocRefId(CorrDocRefId(DocRefId("oldone")))(
          fakePutRequest.withJsonBody(JsString("DocRefId")))
        status(result) shouldBe Status.BAD_REQUEST
      }
      "respond with a BadRequest when the DocRefId is not unique" in {
        when(repo.save(any(), any()))
          .thenReturn(Future.successful(DocRefIdResponses.Valid -> Some(DocRefIdResponses.AlreadyExists)))
        val result = controller.saveCorrDocRefId(CorrDocRefId(DocRefId("oldone")))(
          fakePutRequest.withJsonBody(JsString("DocRefid")))
        status(result) shouldBe Status.BAD_REQUEST
      }
      "respond with a 500 if mongo fails" in {
        when(repo.save(any(), any()))
          .thenReturn(Future.successful(DocRefIdResponses.Valid -> Some(DocRefIdResponses.Failed)))
        val result = controller.saveCorrDocRefId(CorrDocRefId(DocRefId("oldone")))(
          fakePutRequest.withJsonBody(JsString("DocRefid")))
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "respond with a 500 if returns none" in {
        when(repo.save(any(), any())).thenReturn(Future.successful(DocRefIdResponses.Valid -> None))
        val result = controller.saveCorrDocRefId(CorrDocRefId(DocRefId("oldone")))(
          fakePutRequest.withJsonBody(JsString("DocRefid")))
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    "be able to delete a DocRefId" when {
      val runMode = mock[RunMode]
      when(runMode.env) thenReturn "Dev"
      val controller =
        new DocRefIdController(
          repo,
          Configuration("Dev.CBCId.enableTestApis" -> true).withFallback(config),
          cBCRAuth,
          runMode,
          cc)
      "it exists and return a 200" in {
        when(repo.delete(any()))
          .thenReturn(Future.successful(UpdateWriteResult(true, 1, 1, Seq.empty, Seq.empty, None, None, None)))
        val result = controller.deleteDocRefId(DocRefId("stuff"))(fakeDeleteRequest)
        status(result) shouldBe Status.OK
      }
      "it doesn't exist and return a 404" in {
        when(repo.delete(any()))
          .thenReturn(Future.successful(UpdateWriteResult(true, 0, 0, Seq.empty, Seq.empty, None, None, None)))
        val result = controller.deleteDocRefId(DocRefId("stuff"))(fakeDeleteRequest)
        status(result) shouldBe Status.NOT_FOUND
      }
      "something goes wrong return a 500" in {
        when(repo.delete(any()))
          .thenReturn(Future.successful(UpdateWriteResult(false, 0, 0, Seq.empty, Seq.empty, None, None, None)))
        val result = controller.deleteDocRefId(DocRefId("stuff"))(fakeDeleteRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "return NOT_IMPLEMENTED if enableTestApis = false" in {
        val controller = new DocRefIdController(
          repo,
          config.withFallback(Configuration("Dev.CBCId.enableTestApis" -> false)),
          cBCRAuth,
          runMode,
          cc)
        val result = controller.deleteDocRefId(DocRefId("stuff"))(fakeDeleteRequest)
        status(result) shouldBe Status.NOT_IMPLEMENTED
      }
    }
  }
}
