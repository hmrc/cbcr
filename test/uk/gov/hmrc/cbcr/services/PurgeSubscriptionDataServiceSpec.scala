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

import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, MessageRefIdRepository, ReportingEntityDataRepo}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class PurgeSubscriptionDataServiceSpec extends UnitSpec with MockitoSugar with MockAuth with OneAppPerSuite {

  val config = app.injector.instanceOf[Configuration]

  "Delete all the submission data " when {

    "the CBCId.purgeSubscriptionData is true" in {

      val newConfig = config ++ Configuration("CBCId.purgeSubscriptionData" -> true)
      val mockDocRefRepo = mock[DocRefIdRepository]
      val mockReportingEntityRepo = mock[ReportingEntityDataRepo]
      val mockMessageRefIdRepo = mock[MessageRefIdRepository]

      when(mockDocRefRepo.deleteAll).thenReturn(Future.successful(true))
      when(mockReportingEntityRepo.deleteAll).thenReturn(Future.successful(true))
      when(mockMessageRefIdRepo.deleteAll).thenReturn(Future.successful(true))

      new PurgeSubscriptionDataService(newConfig, mockReportingEntityRepo, mockDocRefRepo, mockMessageRefIdRepo)

      verify(mockDocRefRepo, times(1)).deleteAll
      verify(mockReportingEntityRepo, times(1)).deleteAll
      verify(mockMessageRefIdRepo, times(1)).deleteAll
    }

    "Do not delete any data when the CBCId.purgeSubscriptionData is false" in {
      val mockDocRefRepo = mock[DocRefIdRepository]
      val mockReportingEntityRepo = mock[ReportingEntityDataRepo]
      val mockMessageRefIdRepo = mock[MessageRefIdRepository]

      when(mockDocRefRepo.deleteAll).thenReturn(Future.successful(true))
      when(mockReportingEntityRepo.deleteAll).thenReturn(Future.successful(true))
      when(mockMessageRefIdRepo.deleteAll).thenReturn(Future.successful(true))

      new PurgeSubscriptionDataService(config, mockReportingEntityRepo, mockDocRefRepo, mockMessageRefIdRepo)

      verify(mockDocRefRepo, times(0)).deleteAll
      verify(mockReportingEntityRepo, times(0)).deleteAll
      verify(mockMessageRefIdRepo, times(0)).deleteAll
    }

  }
}
