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

import org.mockito.Mockito.{times, verify, when, never, reset}
import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Matchers.any
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class AuditSubscriptionServiceSpec extends UnitSpec with MockitoSugar with MockAuth with OneAppPerSuite with Eventually {
  val config = app.injector.instanceOf[Configuration]
  implicit val ec = app.injector.instanceOf[ExecutionContext]


  val runMode = mock[RunMode]
  val subscriptionDataRepo = mock[SubscriptionDataRepository]
  val mockAudit = mock[AuditConnectorI]

  private val cbcid1 = CBCId("XFCBC0100000026")
  private val cbcid2 = CBCId("XGCBC0100000027")
  private val phoneNumber = PhoneNumber("02072653787")
  private val email = EmailAddress("dave@dave.com")

  val testConfigTrue = Configuration("Dev.audit.subscriptions" -> true)
  val testConfigFalse = Configuration("Dev.audit.subscriptions" -> false)
  val testConfigCbcIds = Configuration("Dev.audit.cbcIds" -> "XFCBC0100000026_XGCBC0100000027")
  val bpr = BusinessPartnerRecord("MySafeID", Some(OrganisationResponse("Dave Corp")), EtmpAddress("13 Accacia Ave", None, None, None, None, "GB"))
  val subscrptionDetails1 = SubscriptionDetails(bpr, SubscriberContact(name = None, Some("Dave"), Some("Jones"), phoneNumber.get, email), cbcid1, Utr("utr"))
  val subscrptionDetails2 = SubscriptionDetails(bpr, SubscriberContact(name = None, Some("Bob"), Some("Smith"), phoneNumber.get, email), cbcid2, Utr("utr"))
  val sd: List[SubscriptionDetails] = List(subscrptionDetails1, subscrptionDetails2)

  when(runMode.env) thenReturn "Dev"
  when(subscriptionDataRepo.getSubscriptions(any())) thenReturn Future.successful(sd)


  "if Dev.audit.subscriptions = true then call SubscriptionDataRepository.getSubscriptions with the list of cbcIds in Dev.audit.cbcIds" should {
    new AuditSubscriptionService(subscriptionDataRepo, config ++ testConfigTrue ++ testConfigCbcIds, runMode, mockAudit)
    "call getSubscriptions to the SubscriptionDataRepository" in {
      verify(subscriptionDataRepo,times(1)).getSubscriptions(any())
    }
    "make an audit call" in {
      when(mockAudit.sendExtendedEvent(any())(any(),any())) thenReturn Future.successful(AuditResult.Success)
      eventually(timeout(6.seconds), interval(15.millis)){ verify(mockAudit, times(1)).sendExtendedEvent(any())(any(),any()) }
    }
  }

  "if Dev.audit.subscriptions = false then dont call SubscriptionDataRepository.getSubscriptions" should {
    "call getSubscriptions to the SubscriptionDataRepository" in {
      reset(subscriptionDataRepo)
      reset(mockAudit)
      new AuditSubscriptionService(subscriptionDataRepo, config ++ testConfigFalse ++ testConfigCbcIds, runMode, mockAudit)
      eventually(verify(subscriptionDataRepo,never()).getSubscriptions(any()))
    }
  }
}