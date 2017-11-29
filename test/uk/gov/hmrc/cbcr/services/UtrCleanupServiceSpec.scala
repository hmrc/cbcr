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

import cats.data._
import cats.implicits._
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UtrCleanupServiceSpec extends UnitSpec with MockitoSugar with MockAuth with OneAppPerSuite with Eventually with BeforeAndAfterEach{

  val okResult = DefaultWriteResult(true,1,Seq.empty,None,None,None)
  val bpr = BusinessPartnerRecord("MySafeID",Some(OrganisationResponse("Dave Corp")),EtmpAddress("13 Accacia Ave",None,None,None,None,"GB"))
  private val cbcid = CBCId("XGCBC0000000001")
  private val phoneNumber = PhoneNumber("02072653787")
  private val email = EmailAddress("dave@dave.com")

  val runMode = mock[RunMode]


  private val validUtr = Utr("8000000010")
  val config = app.injector.instanceOf[Configuration]
  val exampleSubscriptionData = SubscriptionDetails(bpr,SubscriberContact(name = None, Some("Dave"), Some("Jones"), phoneNumber.get,email),cbcid,validUtr)

  override def afterEach(): Unit = {
    super.afterEach()
  }

  "The Utr cleanup service" should {

    "audit utr when audit has been set to true and UTR is valid" in {
      val store = mock[SubscriptionDataRepository]
      val mockAudit = mock[AuditConnector]
      when(store.get(any(classOf[Utr]))) thenReturn OptionT.some[Future, SubscriptionDetails](exampleSubscriptionData)
      when(runMode.env) thenReturn "Dev"
      when(mockAudit.sendExtendedEvent(any())(any(),any())) thenReturn Future[AuditResult](AuditResult.Success)

      new UtrCleanupService(store, config ++ Configuration("Dev.UTR.audit" -> true, "Dev.UTR.delete" -> false, "Dev.UTR.utrs" -> "8000000010"), runMode) {
        override lazy val audit: AuditConnector = mockAudit
      }

      eventually {
        verify(store, times(1)).get(validUtr)
        verify(store, times(0)).clear(validUtr)
        verify(mockAudit, times(1)).sendExtendedEvent(any())(any(), any())
      }

    }

    "audit and delete utr when audit=true, delete=true and UTR is valid" in {
      val store = mock[SubscriptionDataRepository]
      val mockAudit = mock[AuditConnector]
      when(store.get(any(classOf[Utr]))) thenReturn OptionT.some[Future, SubscriptionDetails](exampleSubscriptionData)
      when(store.clear(any(classOf[Utr]))) thenReturn (Future.successful(okResult))
      when(runMode.env) thenReturn "Dev"
      when(mockAudit.sendExtendedEvent(any())(any(),any())) thenReturn Future[AuditResult](AuditResult.Success)

      new UtrCleanupService(store, config ++ Configuration("Dev.UTR.audit" -> true, "Dev.UTR.delete" -> true, "Dev.UTR.utrs" -> "8000000010"), runMode) {
        override lazy val audit: AuditConnector = mockAudit
      }

      eventually {
        verify(store, times(1)).get(validUtr)
        verify(store, times(1)).clear(validUtr)
        verify(mockAudit, times(2)).sendExtendedEvent(any())(any(), any())
      }
    }

    "audit and delete 2 utrs when audit=true, delete=true and Dev.UTRutrs contains 2 valid utr" in {
      val store = mock[SubscriptionDataRepository]
      val mockAudit = mock[AuditConnector]
      when(store.get(any(classOf[Utr]))) thenReturn OptionT.some[Future, SubscriptionDetails](exampleSubscriptionData)
      when(store.clear(any(classOf[Utr]))) thenReturn (Future.successful(okResult))
      when(runMode.env) thenReturn "Dev"
      when(mockAudit.sendExtendedEvent(any())(any(),any())) thenReturn Future[AuditResult](AuditResult.Success)

      new UtrCleanupService(store, config ++ Configuration("Dev.UTR.audit" -> true, "Dev.UTR.delete" -> true, "Dev.UTR.utrs" -> "8000000010, 3000000018"), runMode) {
        override lazy val audit: AuditConnector = mockAudit
      }

      eventually {
        verify(store, times(2)).get(any[Utr])
        verify(store, times(2)).clear(any())
        verify(mockAudit, times(4)).sendExtendedEvent(any())(any(), any())
      }
    }

    "audit but do NOT delete utr when audit=true, delete=true, UTR is valid but audit fails" in {
      val store = mock[SubscriptionDataRepository]
      val mockAudit = mock[AuditConnector]
      when(store.get(any(classOf[Utr]))) thenReturn OptionT.some[Future, SubscriptionDetails](exampleSubscriptionData)
      when(store.clear(any(classOf[Utr]))) thenReturn (Future.successful(okResult))
      when(runMode.env) thenReturn "Dev"
      when(mockAudit.sendExtendedEvent(any())(any(),any())) thenReturn Future[AuditResult](AuditResult.Failure("Stuff"))


      new UtrCleanupService(store, config ++ Configuration("Dev.UTR.audit" -> true, "Dev.UTR.delete" -> true, "Dev.UTR.utrs" -> "8000000010"), runMode) {
        override lazy val audit: AuditConnector = mockAudit
      }

      eventually {
        verify(store, times(1)).get(validUtr)
        verify(store, times(0)).clear(validUtr)
        verify(mockAudit, times(1)).sendExtendedEvent(any())(any(), any())
      }
    }
  }
}
