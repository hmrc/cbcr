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

package uk.gov.hmrc.cbcr.services

import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class SubscriptionDataUpdateServiceSpec extends UnitSpec with MockitoSugar with MockAuth with OneAppPerSuite with Eventually{

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val encodedSC_1 = "eyJmaXJzdE5hbWUiOiAiQm9iIiwgImxhc3ROYW1lIjogIkJvYmJ5IiwgInBob25lTnVtYmVyIjogIjEyMzQ1NiIsICJlbWFpbCI6ICJiaWcuYmFkQHdvbGYuY29tIn0="
  val encodedSC_2 = "eyJmaXJzdE5hbWUiOiAiRmx1ZmZ5IiwgImxhc3ROYW1lIjogIkRvb2RsZSIsICJwaG9uZU51bWJlciI6ICIxMjEyMTIiLCAiZW1haWwiOiAiYmlnLmZsdWZmeUBkb29kbGUuY29tIn0="
  val safeId_1 = "XX0000100094187"
  val safeId_2 = "XG0000100094185"



  "attempt to update Subscription_Details n times" when {

    "performMigration has been set to true and count(n) is 0" in {

      val backupSvc = mock[BackupService]
      val store = mock[SubscriptionDataRepository]
      val config = app.injector.instanceOf[Configuration]
      val audit = mock[AuditConnectorI]
      when(store.update(any(), any())).thenReturn(Future.successful(true))

      val sdus = new SubscriptionDataUpdateService(store, config ++ Configuration("CBCId.performDataUpdate" -> true, "users.count" -> 0),audit)

      eventually {
        verify(store, times(0)).update(any(),any())
      }
    }

    "performMigration has been set to true and count and count(n) is 2" in  {

      val backupSvc = mock[BackupService]
      val store = mock[SubscriptionDataRepository]
      val config = app.injector.instanceOf[Configuration]
      val audit = mock[AuditConnectorI]
      when(store.update(any(), any())).thenReturn(Future.successful(true))

      new SubscriptionDataUpdateService(store, config ++ Configuration("CBCId.performDataUpdate" -> true,
                                                                       "users.count" -> 2,
                                                                       "user1.safeId" -> safeId_1,
                                                                       "user1.sc" -> encodedSC_1,
                                                                       "user2.safeId" -> safeId_2,
                                                                       "user2.sc" -> encodedSC_2
                                                                       ),audit)

      eventually {
        verify(store, times(2)).update(any(),any())
      }

    }


    "performMigration has been set to false" in {

      val backupSvc = mock[BackupService]
      val store = mock[SubscriptionDataRepository]
      val config = app.injector.instanceOf[Configuration]
      val audit = mock[AuditConnectorI]
      when(store.update(any(), any())).thenReturn(Future.successful(true))

      val sdus = new SubscriptionDataUpdateService(store, config, audit)

      eventually {
        verify(store, times(0)).update(any(),any())
      }
    }

  }
}


