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

package uk.gov.hmrc.cbcr.services

import java.time.LocalDate

import akka.stream.Materializer
import cats.data.NonEmptyList
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.cbcr.models.DocRefIdResponses.{AlreadyExists, Failed, Ok}
import uk.gov.hmrc.cbcr.models._
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
  private val reportingPeriod = "2022-02-22"
  private val cbcId = CBCId.create(56).toOption

  private val docRefId = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP")
  private val adminRED = AdminReportingEntityData(List(docRefId), None, docRefId)
  private val red = ReportingEntityData(
    NonEmptyList(docRefId, Nil),
    List(docRefId),
    docRefId,
    TIN("90000000001", "GB"),
    UltimateParentEntity("Foo Corp"),
    CBC701,
    Some(LocalDate.now()),
    Some(LocalDate.now()),
    None,
    None
  )
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  "showAllDocRef" in {
    val docRefIdRecord = DocRefIdRecord(docRefId, valid = true)
    when(docRefIdRepo.findAll()) thenReturn Future.successful(Seq(docRefIdRecord))
    val result = adminService.showAllDocRef(fakeRequest)
    verifyStatusCode(result, Status.OK)
  }

}
