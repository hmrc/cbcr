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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when, _}
import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.{ExecutionContext, Future}

class AuditSubscriptionServiceSpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite with Eventually {

  val config = app.injector.instanceOf[Configuration]
  implicit val ec = app.injector.instanceOf[ExecutionContext]

  val runMode = mock[RunMode]
  val subscriptionDataRepo = mock[SubscriptionDataRepository]
  val mockAudit = mock[AuditConnector]

  val cbcId = CBCId.create(1).getOrElse(fail("Couldn't generate cbcid"))
  val cbcId2 = CBCId.create(2).getOrElse(fail("Couldn't generate cbcid"))
  val utr = Utr("7000000002")
  val testConfig = Configuration("Dev.audit.subscriptions" -> true)
  val cbcIdConfig = Configuration("Dev.audit.cbcIds"       -> s"${cbcId.toString}_${cbcId2.toString}")
  val subscriberContact =
    SubscriberContact(None, "firstName", "lastName", PhoneNumber("07777888899").get, EmailAddress("bob@bob.com"))
  val address = EtmpAddress("address1", Some("address2"), Some("address3"), Some("address4"), Some("PO1 1OP"), "UK")
  val subscriptionDetails = SubscriptionDetails(
    BusinessPartnerRecord("safeId", Some(OrganisationResponse("Org1")), address),
    subscriberContact,
    Some(cbcId),
    utr)
  val subscriptionDetails2 = SubscriptionDetails(
    BusinessPartnerRecord("safeId", Some(OrganisationResponse("Org1")), address),
    subscriberContact,
    Some(cbcId2),
    utr)

  when(runMode.env) thenReturn "Dev"
  when(subscriptionDataRepo.getSubscriptions(any())) thenReturn Future.successful[List[SubscriptionDetails]](
    List(subscriptionDetails, subscriptionDetails2))
  when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Success)

  new AuditSubscriptionService(
    subscriptionDataRepo,
    config.withFallback(testConfig).withFallback(cbcIdConfig),
    runMode,
    mockAudit)
  "Calls to getSubscriptios" should {
    "complete and call audit for each subcription returned " in {

      eventually { verify(mockAudit, times(2)).sendExtendedEvent(any())(any(), any()) }
    }
    "complete and audit fails" in {
      reset(subscriptionDataRepo)
      reset(mockAudit)
      when(subscriptionDataRepo.getSubscriptions(any())) thenReturn Future.successful[List[SubscriptionDetails]](
        List(subscriptionDetails))
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(
        AuditResult.Failure("failed audit", None))

      new AuditSubscriptionService(
        subscriptionDataRepo,
        config.withFallback(testConfig).withFallback(cbcIdConfig),
        runMode,
        mockAudit)
      eventually { verify(mockAudit, times(1)).sendExtendedEvent(any())(any(), any()) }
    }
    "complete and audit disabled" in {
      reset(subscriptionDataRepo)
      reset(mockAudit)
      when(subscriptionDataRepo.getSubscriptions(any())) thenReturn Future.successful[List[SubscriptionDetails]](
        List(subscriptionDetails))
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(AuditResult.Disabled)

      new AuditSubscriptionService(
        subscriptionDataRepo,
        config.withFallback(testConfig).withFallback(cbcIdConfig),
        runMode,
        mockAudit)
      eventually { verify(mockAudit, times(1)).sendExtendedEvent(any())(any(), any()) }
    }
    "complete and audit throws error" in {
      reset(subscriptionDataRepo)
      reset(mockAudit)
      when(subscriptionDataRepo.getSubscriptions(any())) thenReturn Future.successful[List[SubscriptionDetails]](
        List(subscriptionDetails))
      when(mockAudit.sendExtendedEvent(any())(any(), any())) thenReturn Future.failed(new Throwable("audit error"))

      new AuditSubscriptionService(
        subscriptionDataRepo,
        config.withFallback(testConfig).withFallback(cbcIdConfig),
        runMode,
        mockAudit)
      eventually { verify(mockAudit, times(1)).sendExtendedEvent(any())(any(), any()) }
    }

  }
}
