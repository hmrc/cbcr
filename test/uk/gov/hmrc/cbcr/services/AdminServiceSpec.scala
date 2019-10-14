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

package uk.gov.hmrc.cbcr.services

import java.time.LocalDate

import akka.stream.Materializer
import akka.util.ByteString
import cats.data.NonEmptyList
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.{HttpEntity, Status}
import play.api.libs.json.Json
import play.api.libs.streams.Accumulator
import play.api.mvc.{ResponseHeader, Result}
import play.api.test.Helpers.stubControllerComponents
import play.api.test.FakeRequest
import reactivemongo.api.ReadPreference.Primary
import uk.gov.hmrc.cbcr.models.DocRefIdResponses.{AlreadyExists, Failed, Ok}
import uk.gov.hmrc.cbcr.models.{CBC701, CBCId, DocRefId, DocRefIdRecord, ReportingEntityData, TIN, UltimateParentEntity}
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, ReactiveDocRefIdRepository, ReportingEntityDataRepo}
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

class AdminServiceSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  private val repo = mock[ReportingEntityDataRepo]
  private val docRefIdRepo = mock[ReactiveDocRefIdRepository]
  private val config = mock[Configuration]
  private val docRepo = mock[DocRefIdRepository]
  private val runMode = new RunMode(config)
  private val audit = mock[AuditConnector]
  private val cc = stubControllerComponents()

  private val adminService = new AdminService(docRefIdRepo, config, repo, docRepo, runMode, audit, cc)
  private val fakeRequest = FakeRequest()
  private val tin = "tin"
  private val reportingPeriod = "aReportingPeriod"
  private val cbcId = CBCId.create(56).toOption

  private val docRefId = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP")
  private val adminRED = AdminReportingEntityData(List(docRefId), None, docRefId)
  private val red = ReportingEntityData(NonEmptyList(docRefId, Nil), List(docRefId), docRefId, TIN("90000000001", "GB"), UltimateParentEntity("Foo Corp"), CBC701, Some(LocalDate.now()), Some(LocalDate.now()))
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  "adminQueryTin" should {
    "return a notFound response" in {
      when(repo.queryTIN(any(), any())) thenReturn Future.successful(List[ReportingEntityData]())

      val result = adminService.adminQueryTin(tin, reportingPeriod)(fakeRequest)
      verifyStatusCode(result, Status.NOT_FOUND)
    }

    "return an OK response, with a json body containing reportEntityDataModel" in {
      when(repo.queryTIN(any(), any())) thenReturn Future.successful(List(red))
      val result = adminService.adminQueryTin(tin, reportingPeriod)(fakeRequest)
      verifyStatusCode(result, Status.OK)
      verifyResult(result, red)
    }

    "recover from a future failed and return an internalServerError" in {
      when(repo.queryTIN(any(), any())) thenReturn Future.failed(new Exception("mugged it"))
      val result = adminService.adminQueryTin(tin, reportingPeriod)(fakeRequest)
      verifyStatusCode(result, Status.INTERNAL_SERVER_ERROR)
    }
  }

  "adminQueryCbcId" should {
    "respond with a 404 when asked to retrieve a non-existent ReportingEntityData for given cbd-id and reporting-period" in {
      when(repo.queryCbcId(any[CBCId], any())) thenReturn Future.successful(None)
      val result = adminService.adminQueryCbcId(cbcId.get, LocalDate.now().toString)(fakeRequest)
      verifyStatusCode(result, Status.NOT_FOUND)
    }

    "respond with a 200 when asked to retrieve an existing ReportingEntityData for given cbc-id and reporting-period" in {
      when(repo.queryCbcId(any[CBCId], any())) thenReturn Future.successful(Some(red))
      val result = adminService.adminQueryCbcId(cbcId.get, LocalDate.now().toString)(fakeRequest)
      verifyStatusCode(result, Status.OK)
    }

    "respond with a 500 if error when checking ReportingEntityData for given cbc-id and reporting-period" in {
      when(repo.queryCbcId(any[CBCId], any())) thenReturn Future.failed(new Exception("bad"))
      val result = adminService.adminQueryCbcId(cbcId.get, LocalDate.now().toString)(fakeRequest)
      verifyStatusCode(result, Status.INTERNAL_SERVER_ERROR)
    }
  }

  "adminDocRefIdQuery" should {
    "respond with a 404 code" in {
      when(repo.query(any[DocRefId])) thenReturn Future.successful(None)
      val result = adminService.adminDocRefIdquery(docRefId)(fakeRequest)
      verifyStatusCode(result, Status.NOT_FOUND)
    }

    "respond with a 200 code" in {
      when(repo.query(any[DocRefId])) thenReturn Future.successful(Some(red))
      val result = adminService.adminDocRefIdquery(docRefId)(fakeRequest)
      verifyStatusCode(result, Status.OK)
      verifyResult(result, red)
    }

    "respond with an internal server error when querying a docRefId" in {
      when(repo.query(any[DocRefId]())) thenReturn Future.failed(new Exception("bad"))
      val result = adminService.adminDocRefIdquery(docRefId)(fakeRequest)
      verifyStatusCode(result, Status.INTERNAL_SERVER_ERROR)
    }
  }


  "editDocRefId" should {
    "respond with a 200 code" in {
      when(docRepo.edit(any())) thenReturn Future.successful(200)
      val result = adminService.editDocRefId(docRefId)(fakeRequest)
      verifyStatusCode(result, Status.OK)
    }
    "respond with a NotModified response if the status code is less than 1" in {
      when(docRepo.edit(any())) thenReturn Future.successful(0)
      val result = adminService.editDocRefId(docRefId)(fakeRequest)
      verifyStatusCode(result, Status.NOT_MODIFIED)
    }
  }

  "saveDocRefId" should {
    "respond with a 200 response code" in {
      when(docRepo.save(any())) thenReturn Future.successful(Ok)
      val result = adminService.saveDocRefId(docRefId)(fakeRequest)
      verifyStatusCode(result, Status.OK)
    }

    "respond with a Conflict response code when the docRefId already exists" in {
      when(docRepo.save(any())) thenReturn Future.successful(AlreadyExists)
      val result = adminService.saveDocRefId(docRefId)(fakeRequest)
      verifyStatusCode(result, Status.CONFLICT)
    }

    "respond with an internalServerError" in {
      when(docRepo.save(any())) thenReturn Future.successful(Failed)
      val result = adminService.saveDocRefId(docRefId)(fakeRequest)
      verifyStatusCode(result, Status.INTERNAL_SERVER_ERROR)
    }
  }

  "showAllDocRef" in {
    val docRefIdRecord = DocRefIdRecord(docRefId, valid = true)
    when(docRefIdRepo.findAll(any())(any())) thenReturn Future.successful(List(docRefIdRecord))
    val result = adminService.showAllDocRef(fakeRequest)
    verifyStatusCode(result, 200)
  }

  "editReportingEntityData" should {
    "return an 200 response code when the reporting entity DRI has been updated" in {
      when(repo.updateReportingEntityDRI(any(), any())) thenReturn Future.successful(true)
      val result = adminService.editReportingEntityData(docRefId)(fakeRequest.withBody(Json.toJson(adminRED)))
      verifyStatusCode(result, Status.OK)
    }
  }



