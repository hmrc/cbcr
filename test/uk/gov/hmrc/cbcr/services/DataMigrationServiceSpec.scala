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
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.controllers.{MockAuth, SubscriptionDataController}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DataMigrationServiceSpec  extends UnitSpec with MockitoSugar with MockAuth with OneAppPerSuite with Eventually{


  val bpr = BusinessPartnerRecord("MySafeID",Some(OrganisationResponse("Dave Corp")),EtmpAddress("13 Accacia Ave",None,None,None,None,"GB"))
  private val cbcid = CBCId("XGCBC0000000001")
  private val phoneNumber = PhoneNumber("02072653787")
  private val email = EmailAddress("dave@dave.com")

  val exampleSubscriptionData = SubscriptionDetails(bpr,SubscriberContact(name = None, Some("Dave"), Some("Jones"), phoneNumber.get,email),cbcid,Utr("utr"))
  var subscriberContact: SubscriberContact = SubscriberContact(name = Some("Michael John Joseph Junior"), None, None, phoneNumber.get,EmailAddress("dave@dave.com"))
  val exampleSubscriptionDataNameOnly = SubscriptionDetails(bpr,subscriberContact,cbcid,Utr("utr"))
  var subscriberContactFixed: SubscriberContact = SubscriberContact(name = None, Some("Michael John Joseph"), Some("Junior"), phoneNumber.get,EmailAddress("dave@dave.com"))


  var subscriberContact2: SubscriberContact = SubscriberContact(name = Some("Michael Joseph"), None, None, phoneNumber.get,EmailAddress("dave@dave.com"))
  val exampleSubscriptionDataNameOnly2 = SubscriptionDetails(bpr,subscriberContact2,cbcid,Utr("utr"))
  var subscriberContactFixed2: SubscriberContact = SubscriberContact(name = None, Some("Michael"), Some("Joseph"), phoneNumber.get,EmailAddress("dave@dave.com"))


  var subscriberContact3: SubscriberContact = SubscriberContact(name = Some("Joseph"), None, None, phoneNumber.get,EmailAddress("dave@dave.com"))
  val exampleSubscriptionDataNameOnly3 = SubscriptionDetails(bpr,subscriberContact3,cbcid,Utr("utr"))
  var subscriberContactFixed3: SubscriberContact = SubscriberContact(name = None, Some("Joseph"), Some("Joseph"), phoneNumber.get,EmailAddress("dave@dave.com"))

  val config = app.injector.instanceOf[Configuration]


//  "attempt to migrate all the Subscription_Details that have been locally generated" when {
//
//    "performMigration has been set to true " in {
//
//      val store = mock[SubscriptionDataRepository]
//      val desConnector = mock[DESConnector]
//      when(store.getSubscriptions(DataMigrationCriteria.LOCAL_CBCID_CRITERIA)) thenReturn Future.successful(List(exampleSubscriptionData, exampleSubscriptionData, exampleSubscriptionData))
//      when(desConnector.createMigration(any())) thenReturn Future.successful(HttpResponse(responseStatus = 200))
//
//      new DataMigrationService(store, desConnector, config ++ Configuration("CBCId.performMigration" -> true))
//
//
//      eventually {
//        verify(desConnector, times(3)).createMigration(any())
//      }
//
//    }
//  }
//
//  "not attempt to migrate all the Subscription_Details that have been locally generated" when {
//
//    "performMigration has not been explicitly set to true" in {
//
//      val desConnector = mock[DESConnector]
//      val store = mock[SubscriptionDataRepository]
//      when(store.getSubscriptions(DataMigrationCriteria.LOCAL_CBCID_CRITERIA)) thenReturn Future.successful(List(exampleSubscriptionData, exampleSubscriptionData, exampleSubscriptionData))
//      new DataMigrationService(store, desConnector, config)
//
//      eventually{verify(desConnector, times(0)).createMigration(any())}
//    }
//  }
//
//  "attempt to fix the data model for Subscriber Contact splitting name into first name last name and updating the store multiple names Michael John Joseph Junior" when {
//    "CBCId.doFirstNameLastNameDataFix is true" in {
//
//      val desConnector = mock[DESConnector]
//      val store = mock[SubscriptionDataRepository]
//      when(store.getSubscriptions(DataMigrationCriteria.NAME_SPLIT_CRITERIA)) thenReturn Future.successful(List(exampleSubscriptionDataNameOnly))
//      when(store.update(any(), any())).thenReturn(Future.successful(true))
//
//      new DataMigrationService(store, desConnector, config ++ Configuration("CBCId.doFirstNameLastNameDataFix" -> true))
//
//      eventually{verify(store, times(1)).getSubscriptions(DataMigrationCriteria.NAME_SPLIT_CRITERIA)}
//      eventually{verify(store, times(1)).update(cbcid.get, subscriberContactFixed)}
//
//
//    }
//  }
//
//  "attempt to fix the data model for Subscriber Contact splitting name into first name last name and updating the store normal name Michael Joseph" when {
//    "CBCId.doFirstNameLastNameDataFix is true" in {
//
//      val desConnector = mock[DESConnector]
//      val store = mock[SubscriptionDataRepository]
//      when(store.getSubscriptions(DataMigrationCriteria.NAME_SPLIT_CRITERIA)) thenReturn Future.successful(List( exampleSubscriptionDataNameOnly2))
//      when(store.update(any(), any())).thenReturn(Future.successful(true))
//
//      new DataMigrationService(store, desConnector, config ++ Configuration("CBCId.doFirstNameLastNameDataFix" -> true))
//
//      eventually{verify(store, times(1)).getSubscriptions(DataMigrationCriteria.NAME_SPLIT_CRITERIA)}
//      eventually{verify(store, times(1)).update(cbcid.get, subscriberContactFixed2)}
//
//    }
//  }
//
//  "attempt to fix the data model for Subscriber Contact splitting name into first name last name and updating the store single name Joseph" when {
//    "CBCId.doFirstNameLastNameDataFix is true" in {
//
//      val desConnector = mock[DESConnector]
//      val store = mock[SubscriptionDataRepository]
//      when(store.getSubscriptions(DataMigrationCriteria.NAME_SPLIT_CRITERIA)) thenReturn Future.successful(List(exampleSubscriptionDataNameOnly3))
//      when(store.update(any(), any())).thenReturn(Future.successful(true))
//
//      new DataMigrationService(store, desConnector, config ++ Configuration("CBCId.doFirstNameLastNameDataFix" -> true))
//
//      eventually{verify(store, times(1)).getSubscriptions(DataMigrationCriteria.NAME_SPLIT_CRITERIA)}
//      eventually{verify(store, times(1)).update(cbcid.get, subscriberContactFixed3)}
//
//    }
//  }
//
//
//    " do not attempt to fix the data model for Subscriber Contact" when {
//    "CBCId.doFirstNameLastNameDataFix is false" in {
//      val desConnector = mock[DESConnector]
//      val store = mock[SubscriptionDataRepository]
//      when(store.getSubscriptions(DataMigrationCriteria.NAME_SPLIT_CRITERIA)) thenReturn Future.successful(List(exampleSubscriptionDataNameOnly, exampleSubscriptionDataNameOnly, exampleSubscriptionDataNameOnly))
//      when(store.update(any(), any())).thenReturn(Future.successful(true))
//
//      new DataMigrationService(store, desConnector, config)
//
//      verify(store, times(0)).getSubscriptions(DataMigrationCriteria.NAME_SPLIT_CRITERIA)
//      verify(store, times(0)).update(any(), any())
//    }
//  }
}
