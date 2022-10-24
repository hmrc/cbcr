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

package uk.gov.hmrc.cbcr.services

import org.scalatest.concurrent.Eventually
import play.api.Configuration
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, ReportingEntityDataRepo}
import uk.gov.hmrc.cbcr.util.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import reactivemongo.api.commands.UpdateWriteResult
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.{ExecutionContext, Future}

class DocRefIdClearServiceSpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite with Eventually {

  val config = app.injector.instanceOf[Configuration]
  implicit val ec = app.injector.instanceOf[ExecutionContext]

  val runMode = mock[RunMode]
  val docRefIdRepo = mock[DocRefIdRepository]
  val reportingEntityDataRepo = mock[ReportingEntityDataRepo]
  val mockAudit = mock[AuditConnector]

  val testConfig = Configuration("Dev.DocRefId.clear" -> "docRefId1_docRefId2_docRefId3_docRefId4")
  val writeResult = UpdateWriteResult(true, 1, 1, Seq.empty, Seq.empty, None, None, None)
  val notFoundWriteResult = UpdateWriteResult(true, 0, 0, Seq.empty, Seq.empty, None, None, None)

  when(runMode.env) thenReturn "Dev"
  when(docRefIdRepo.delete(any())(any())) thenReturn Future.successful(writeResult)
  when(reportingEntityDataRepo.delete(any())) thenReturn Future.successful(writeResult)

  new DocRefIdClearService(docRefIdRepo, reportingEntityDataRepo, config.withFallback(testConfig), runMode, mockAudit)

  "If there are docRefIds in the $RUNMODE.DocRefId.clear field then, for each '_' separated docrefid, it" should {

    "call delete to the DocRefIdRepo" in {
      verify(docRefIdRepo, times(4)).delete(any())(any())
    }

    "call delete to the ReportingEntityDataRepo" in {
      verify(reportingEntityDataRepo, times(4)).delete(any())
    }

    "make an audit call" in {
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Success)
      eventually { verify(mockAudit, times(4)).sendExtendedEvent(any())(any(), any()) }
    }

  }

  "Calls to delete a ReportingEntityData entry that does not exist" should {

    "complete without error" in {
      reset(reportingEntityDataRepo)
      reset(mockAudit)
      when(reportingEntityDataRepo.delete(any())) thenReturn Future.successful(notFoundWriteResult)
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Success)

      new DocRefIdClearService(
        docRefIdRepo,
        reportingEntityDataRepo,
        config.withFallback(testConfig),
        runMode,
        mockAudit)
      eventually { verify(reportingEntityDataRepo, times(4)).delete(any()) }
      eventually { verify(mockAudit, times(4)).sendExtendedEvent(any())(any(), any()) }
    }

    "complete without error but audit fails" in {
      reset(reportingEntityDataRepo)
      reset(mockAudit)
      when(reportingEntityDataRepo.delete(any())) thenReturn Future.successful(notFoundWriteResult)
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(
        AuditResult.Failure("Audit Failure", None))

      new DocRefIdClearService(
        docRefIdRepo,
        reportingEntityDataRepo,
        config.withFallback(testConfig),
        runMode,
        mockAudit)
      eventually { verify(reportingEntityDataRepo, times(4)).delete(any()) }
      eventually { verify(mockAudit, times(4)).sendExtendedEvent(any())(any(), any()) }
    }

    "complete without error but audit disabled" in {
      reset(reportingEntityDataRepo)
      reset(mockAudit)
      when(reportingEntityDataRepo.delete(any())) thenReturn Future.successful(notFoundWriteResult)
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Disabled)

      new DocRefIdClearService(
        docRefIdRepo,
        reportingEntityDataRepo,
        config.withFallback(testConfig),
        runMode,
        mockAudit)
      eventually { verify(reportingEntityDataRepo, times(4)).delete(any()) }
      eventually { verify(mockAudit, times(4)).sendExtendedEvent(any())(any(), any()) }
    }

  }
}
