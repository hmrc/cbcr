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
import org.mockito.Mockito.*
import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.cbcr.config.ApplicationConfig
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.repositories.MessageRefIdRepository
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.{ExecutionContext, Future}

class MsgRefIdClearServiceSpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite with Eventually {

  private implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val config = mock[ApplicationConfig]
  when(config.msgRefIdsToClear).thenReturn(Seq("msgRefId1", "msgRefId2"))

  private val mockAudit = mock[AuditConnector]
  private val msgRefIdRepo = mock[MessageRefIdRepository]

  val writeResult: DeleteResult = DeleteResult.acknowledged(1)

  when(msgRefIdRepo.delete(any())).thenReturn(Future.successful(writeResult))

  when(mockAudit.sendExtendedEvent(any())(using any(), any())).thenReturn(Future.successful(AuditResult.Disabled))

  new MessageRefIdClearService(msgRefIdRepo, config, mockAudit)

  "If there are msgRefIds configured for deletion, for each msgRefId, it" should {

    "call delete to the MsgRefIdRepo" in {
      verify(msgRefIdRepo, times(2)).delete(any())
    }

    "make an audit call" in {
      when(mockAudit.sendExtendedEvent(any())(using any(), any())).thenReturn(Future.successful(AuditResult.Success))
      verify(mockAudit, times(2)).sendExtendedEvent(any())(using any(), any())
    }

  }
}
