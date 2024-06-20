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

import com.mongodb.client.result.DeleteResult
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.cbcr.config.ApplicationConfig
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, ReportingEntityDataRepo}
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.{ExecutionContext, Future}

class DocRefIdClearServiceSpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite with Eventually {

  private val config = mock[ApplicationConfig]
  when(config.docRefIdsToClear).thenReturn("docRefId1_docRefId2_docRefId3_docRefId4")
  private implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val docRefIdRepo = mock[DocRefIdRepository]
  private val reportingEntityDataRepo = mock[ReportingEntityDataRepo]
  private val mockAudit = mock[AuditConnector]

  val writeResult: DeleteResult = DeleteResult.acknowledged(1)

  when(docRefIdRepo.delete(any())) thenReturn Future.successful(writeResult)
  when(reportingEntityDataRepo.delete(any())) thenReturn Future.successful(writeResult)

  // This is now being called and it wasn't bruv
  when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Disabled)

  new DocRefIdClearService(docRefIdRepo, reportingEntityDataRepo, config, mockAudit)

  "If there are docRefIds in the $RUNMODE.DocRefId.clear field then, for each '_' separated docrefid, it" should {

    "call delete to the DocRefIdRepo" in {
      verify(docRefIdRepo, times(4)).delete(any())
    }

    "call delete to the ReportingEntityDataRepo" in {
      verify(reportingEntityDataRepo, times(4)).delete(any())
    }

    "make an audit call" in {
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Success)
      verify(mockAudit, times(4)).sendExtendedEvent(any())(any(), any())
    }

  }

  "Calls to delete a ReportingEntityData entry that does not exist" should {

    "complete without error" in {
      reset(reportingEntityDataRepo)
      reset(mockAudit)
      when(reportingEntityDataRepo.delete(any())) thenReturn Future.failed[DeleteResult](new Exception())
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Success)

      new DocRefIdClearService(docRefIdRepo, reportingEntityDataRepo, config, mockAudit)
      verify(reportingEntityDataRepo, times(4)).delete(any())
      verify(mockAudit, times(4)).sendExtendedEvent(any())(any(), any())
    }

    "complete without error but audit fails" in {
      reset(reportingEntityDataRepo)
      reset(mockAudit)
      when(reportingEntityDataRepo.delete(any())) thenReturn Future.failed[DeleteResult](new Exception())
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(
        AuditResult.Failure("Audit Failure", None)
      )

      new DocRefIdClearService(docRefIdRepo, reportingEntityDataRepo, config, mockAudit)
      verify(reportingEntityDataRepo, times(4)).delete(any())
      verify(mockAudit, times(4)).sendExtendedEvent(any())(any(), any())
    }

    "complete without error but audit disabled" in {
      reset(reportingEntityDataRepo)
      reset(mockAudit)
      when(reportingEntityDataRepo.delete(any())) thenReturn Future.failed[DeleteResult](new Exception())
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Disabled)

      new DocRefIdClearService(docRefIdRepo, reportingEntityDataRepo, config, mockAudit)
      verify(reportingEntityDataRepo, times(4)).delete(any())
      verify(mockAudit, times(4)).sendExtendedEvent(any())(any(), any())
    }

  }
}
