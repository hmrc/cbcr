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
import play.api.libs.json.{JsValue, Json}
import play.api.test.{FakeRequest, Helpers}
import reactivemongo.api.commands.{DefaultWriteResult, WriteError}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.{MessageRefIdRepository, ReportingEntityDataRepo}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class ReportingEntityDataControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures with MockAuth{

  val docRefId=DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP")

  val red = ReportingEntityData(docRefId,Some(docRefId),docRefId,Utr("90000000001"),UltimateParentEntity("Foo Corp"),CBC701)

  val pred = PartialReportingEntityData(Some(DocRefIdPair(docRefId,None)),Some(DocRefIdPair(docRefId,None)),DocRefIdPair(docRefId,None),Utr("90000000001"),UltimateParentEntity("Foo Corp"),CBC701)

  val okResult = DefaultWriteResult(true, 0, Seq.empty, None, None, None)

  val failResult = DefaultWriteResult(false, 1, Seq(WriteError(1, 1, "Error")), None, None, Some("Error"))

  val fakePostRequest : FakeRequest[JsValue]= FakeRequest(Helpers.POST, "/reporting-entity").withBody(Json.toJson(red))

  val fakePutRequest : FakeRequest[JsValue]= FakeRequest(Helpers.PUT, "/reporting-entity").withBody(Json.toJson(pred))

  val fakeGetRequest = FakeRequest(Helpers.GET, "/reporting-entity/myDocRefId")

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global


  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()

  val repo = mock[ReportingEntityDataRepo]

  val controller = new ReportingEntityDataController(repo,cBCRAuth)

  "The MessageRefIdController" should {
    "respond with a 200 when asked to save a ReportingEntityData" in {
      when(repo.save(any())) thenReturn Future.successful(okResult)
      val result     = controller.save()(fakePostRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 500 if there is a DB failure" in {
      when(repo.save(any())).thenReturn(Future.successful(failResult))
      val result = controller.save()(fakePostRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "respond with a 404 when asked to retrieve a non-existent ReportingEntityData" in {
      when(repo.query(any())).thenReturn(Future.successful(None))
      val result = controller.query(DocRefId("docrefid"))(fakeGetRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "respond with a 200 when asked to retrieve an existing ReportingEntityData" in {
      when(repo.query(any())) thenReturn Future.successful(Some(red))
      val result = controller.query(DocRefId("docrefid"))(fakeGetRequest)
      status(result) shouldBe Status.OK
      Await.result(jsonBodyOf(result), 2.seconds) shouldEqual Json.toJson(red)
    }

    "respond with a 303(NOT_MODIFIED) when asked to update a nonexistant ReportingEntityData" in {
      when(repo.update(any())) thenReturn Future.successful(false)
      val result = controller.update()(fakePutRequest)
      status(result) shouldBe Status.NOT_MODIFIED
    }

    "respond with a 200 when successfully updated a ReportingEntityData field" in {
      when(repo.update(any())) thenReturn Future.successful(true)
      val result = controller.update()(fakePutRequest)
      status(result) shouldBe Status.OK
    }

  }

}
