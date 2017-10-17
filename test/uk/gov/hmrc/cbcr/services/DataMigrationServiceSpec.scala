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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.controllers.{MockAuth, SubscriptionDataController}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DataMigrationServiceSpec  extends UnitSpec with MockitoSugar with MockAuth with OneAppPerSuite{



  val store = mock[SubscriptionDataRepository]
  val bpr = BusinessPartnerRecord("MySafeID",Some(OrganisationResponse("Dave Corp")),EtmpAddress("13 Accacia Ave",None,None,None,None,"GB"))
  val exampleSubscriptionData = SubscriptionDetails(bpr,SubscriberContact(name = None, Some("Dave"), Some("Jones"), PhoneNumber("02072653787").get,EmailAddress("dave@dave.com")),CBCId("XGCBC0000000001"),Utr("utr"))
  val config = app.injector.instanceOf[Configuration]


  "attempt to migrate all the Subscription_Details that have been locally generated" when {

    "performMigration has been set to true " in {

      val desConnector = mock[DESConnector]
      when(store.getSubscriptions(DataMigrationCriteria.LOCAL_CBCID_CRITERIA)) thenReturn Future.successful(List(exampleSubscriptionData, exampleSubscriptionData, exampleSubscriptionData))
      when(desConnector.createMigration(any())) thenReturn Future.successful(HttpResponse(responseStatus = 200))

      new DataMigrationService(store, desConnector, config ++ Configuration("CBCId.performMigration" -> true))

      verify(desConnector, times(3)).createMigration(any())

    }
  }

  "not attempt to migrate all the Subscription_Details that have been locally generated" when {

    "performMigration has not been explicitly set to true" in {

      val desConnector = mock[DESConnector]
      when(store.getSubscriptions(DataMigrationCriteria.LOCAL_CBCID_CRITERIA)) thenReturn Future.successful(List(exampleSubscriptionData, exampleSubscriptionData, exampleSubscriptionData))
      new DataMigrationService(store, desConnector, config)

      verify(desConnector, times(0)).createMigration(any())
    }

  }
}
