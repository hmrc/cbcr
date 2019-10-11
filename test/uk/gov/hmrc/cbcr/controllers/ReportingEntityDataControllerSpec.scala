/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.NonEmptyList
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.{FakeRequest, Helpers}
import reactivemongo.api.commands.{DefaultWriteResult, WriteError}
import uk.gov.hmrc.cbcr.models.{CBC701, _}
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import uk.gov.hmrc.cbcr.util.{LogCapturing, UnitSpec}

import scala.concurrent.Future

class ReportingEntityDataControllerSpec extends UnitSpec with ScalaFutures with MockAuth with LogCapturing {

  val docRefId=DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP")

  val red = ReportingEntityData(NonEmptyList(docRefId,Nil),List(docRefId),docRefId,TIN("90000000001","GB"),UltimateParentEntity("Foo Corp"),CBC701,Some(LocalDate.now()),Some(LocalDate.now()))

  val pred = PartialReportingEntityData(List(DocRefIdPair(docRefId,None)),List(DocRefIdPair(docRefId,None)),DocRefIdPair(docRefId,None),TIN("90000000001","GB"),UltimateParentEntity("Foo Corp"),CBC701,Some(LocalDate.now()),Some(LocalDate.now()))

  val redm = ReportingEntityDataModel(NonEmptyList(docRefId,Nil), List(docRefId),docRefId,TIN("90000000001","GB"),UltimateParentEntity("Foo Corp"), CBC701,Some(LocalDate.now()),Some(LocalDate.now()), oldModel = false)

  val okResult = DefaultWriteResult(true, 0, Seq.empty, None, None, None)

  val tin = "tin"

  val reportingPeriod = "aReportingPeriod"

  val failResult = DefaultWriteResult(false, 1, Seq(WriteError(1, 1, "Error")), None, None, Some("Error"))

  val fakePostRequest : FakeRequest[JsValue]= FakeRequest(Helpers.POST, "/reporting-entity").withBody(Json.toJson(red))

  val badFakePostRequest : FakeRequest[JsValue]= FakeRequest(Helpers.POST, "/reporting-entity").withBody(Json.obj("bad" -> "request"))

  val fakePutRequest : FakeRequest[JsValue]= FakeRequest(Helpers.PUT, "/reporting-entity").withBody(Json.toJson(pred))

  val badFakePutRequest : FakeRequest[JsValue]= FakeRequest(Helpers.PUT, "/reporting-entity").withBody(Json.obj("bad" -> "request"))

  val fakeGetRequest = FakeRequest(Helpers.GET, "/reporting-entity/myDocRefId")

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val cbcId = CBCId.create(56).toOption

  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()

  val repo = mock[ReportingEntityDataRepo]

  val controller = new ReportingEntityDataController(repo, cBCRAuth, cc)

  "The MessageRefIdController" should {
    "respond with a 200 when asked to save a ReportingEntityData" in {
      when(repo.save(any())) thenReturn Future.successful(okResult)
      val result     = controller.save()(fakePostRequest)
      verifyStatusCode(result, Status.OK)
    }

    "respond with a 500 if there is a DB failure" in {
      when(repo.save(any())).thenReturn(Future.successful(failResult))
      val result = controller.save()(fakePostRequest)
      verifyStatusCode(result, Status.INTERNAL_SERVER_ERROR)
    }

    "respond with a 400 if ReportingEntity in request is invalid" in {
      val result = controller.save()(badFakePostRequest)
      verifyStatusCode(result, Status.BAD_REQUEST)
    }

    "respond with a 404 when asked to retrieve a non-existent ReportingEntityData for given cbd-id and reporting-period" in {
      when(repo.queryCbcId(any[CBCId], any())).thenReturn(Future.successful(None))
      val result = controller.queryCbcId(cbcId.get, LocalDate.now().toString)(fakeGetRequest)
      verifyStatusCode(result, Status.NOT_FOUND)
    }

    "respond with a 200 when asked to retrieve an existing ReportingEntityData for given cbc-id and reporting-period" in {
      when(repo.queryCbcId(any[CBCId], any())).thenReturn(Future.successful(Some(red)))
      val result = controller.queryCbcId(cbcId.get, LocalDate.now().toString)(fakeGetRequest)
      verifyStatusCode(result, Status.OK)
    }

    "respond with a 500 if error when checking ReportingEntityData for given cbc-id and reporting-period" in {
      when(repo.queryCbcId(any[CBCId], any())).thenReturn(Future.failed(new Exception("bad")))
      val result = controller.queryCbcId(cbcId.get, LocalDate.now().toString)(fakeGetRequest)
      verifyStatusCode(result, Status.INTERNAL_SERVER_ERROR)
    }

    "respond with a 404 when asked to retrieve a non-existent ReportingEntityData" in {
      when(repo.query(any[DocRefId])).thenReturn(Future.successful(None))
      val result = controller.query(DocRefId("docrefid"))(fakeGetRequest)
      verifyStatusCode(result, Status.NOT_FOUND)
    }

    "respond with a 200 when asked to retrieve an existing ReportingEntityData" in {
      when(repo.query(any[DocRefId])) thenReturn Future.successful(Some(red))
      val result = controller.query(DocRefId("docrefid"))(fakeGetRequest)
      verifyStatusCode(result, Status.OK)
      verifyResult(result, red)
    }

    "respond with a 500 if error when checking ReportingEntityData" in {
      when(repo.query(any[DocRefId])) thenReturn Future.failed(new Exception("bad"))
      val result = controller.query(DocRefId("docrefid"))(fakeGetRequest)
      verifyStatusCode(result, Status.INTERNAL_SERVER_ERROR)
    }

    "respond with a 303(NOT_MODIFIED) when asked to update a nonexistant ReportingEntityData" in {
      when(repo.update(any[PartialReportingEntityData])) thenReturn Future.successful(false)
      val result = controller.update()(fakePutRequest)
      verifyStatusCode(result, Status.NOT_MODIFIED)
    }

    "respond with a 200 when successfully updated a ReportingEntityData field" in {
      when(repo.update(any[PartialReportingEntityData])) thenReturn Future.successful(true)
      val result = controller.update()(fakePutRequest)
      verifyStatusCode(result, Status.OK)
    }

    "respond with a 400 if PartialReportingEntityData in request is invalid" in {
      when(repo.update(any[PartialReportingEntityData])) thenReturn Future.successful(true)
      val result = controller.update()(badFakePutRequest)
      verifyStatusCode(result, Status.BAD_REQUEST)
    }

    "respond with a 200 and a reportingEntityData entry if the  reportingEntityDRI matches that provided DocRefId" in {
      when(repo.queryReportingEntity(any())) thenReturn Future.successful(Some(red))
      val result = controller.queryDocRefId(docRefId)(fakeGetRequest)
      verifyStatusCode(result, Status.OK)
      verifyResult(result, red)
    }

    "respond with a 500 when error in mongo query" in {
      when(repo.queryReportingEntity(any())) thenReturn Future.failed(new Exception("bad"))
      val result = controller.queryDocRefId(docRefId)(fakeGetRequest)
      verifyStatusCode(result, Status.INTERNAL_SERVER_ERROR)
    }

    "respond with a 404 if the reportingEntityDRI matches that provided DocRefId" in {
      when(repo.queryReportingEntity(any())) thenReturn Future.successful(None)
      val result = controller.queryDocRefId(docRefId)(fakeGetRequest)
      verifyStatusCode(result, Status.NOT_FOUND)
    }

    "queryTin" should {
      "return a notFound response" in {
        when(repo.queryTIN(any(),any())) thenReturn Future.successful(List[ReportingEntityData]())

        val result = controller.queryTin(tin, reportingPeriod)(fakeGetRequest)
        verifyStatusCode(result, Status.NOT_FOUND)
      }
      "return an OK response, with a json body containing reportEntityData" in {
        when(repo.queryTIN(any(),any())) thenReturn Future.successful(List[ReportingEntityData](red))

        val result = controller.queryTin(tin, reportingPeriod)(fakeGetRequest)
        verifyStatusCode(result, Status.OK)
        verifyResult(result, red)
      }

      "recover from a future failed and return an internalServerError" in {
          when(repo.queryTIN(any(), any())) thenReturn Future.failed(new Exception("mugged it"))
         val result = controller.queryTin(tin, reportingPeriod)(fakeGetRequest)
         verifyStatusCode(result, Status.INTERNAL_SERVER_ERROR)
      }
    }

    "queryModel" should {
      "return a notFound response" in {
        when(repo.queryModel(any())) thenReturn Future.successful(None)
        val result = controller.queryModel(docRefId)(fakeGetRequest)
        verifyStatusCode(result, Status.NOT_FOUND)
      }
      "return an OK response, with a json body containing reportEntityDataModel" in {
        when(repo.queryModel(any())) thenReturn Future.successful(Some(redm))
        val result = controller.queryModel(docRefId)(fakeGetRequest)
        verifyStatusCode(result, Status.OK)
        verifyResult(result, redm)
      }

      "recover from a future failed and return an internalServerError" in {
        when(repo.queryModel(any())) thenReturn Future.failed(new Exception("mugged it"))
        val result = controller.queryModel(docRefId)(fakeGetRequest)
        verifyStatusCode(result, Status.INTERNAL_SERVER_ERROR)
      }
    }
    }
  }


