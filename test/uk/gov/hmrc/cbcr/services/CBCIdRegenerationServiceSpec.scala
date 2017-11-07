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

import org.scalatest.concurrent.Eventually
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.connectors.{DESConnector, EmailConnectorImpl}
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import org.mockito.Matchers._
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.services.CBCIdGenCommands.GenerateCBCIdResponse
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.{AuditResult, AuditConnector => Auditing}

import scala.concurrent.Future

class CBCIdRegenerationServiceSpec  extends UnitSpec with MockitoSugar with MockAuth with OneAppPerSuite with Eventually{

  val mockEmailConnector = mock[EmailConnectorImpl]
  val subDataRepo = mock[SubscriptionDataRepository]
  val des = mock[DESConnector]
  val local = mock[LocalSubscription]
  val config = app.injector.instanceOf[Configuration]
  val auditMock = mock[Auditing]

  val srb1 = SubscriptionDetails(
    BusinessPartnerRecord("SafeID",Some(OrganisationResponse("Name")), EtmpAddress("Some ave",None,None,None,None, "GB")),
    SubscriberContact(name = None, Some("dave1"), Some("jones"), PhoneNumber("123456789").get,EmailAddress("bob@bob1.com")),
    CBCId("XGCBC0000000001"),
    Utr("8000000004")
  )

  val srb2 = SubscriptionDetails(
    BusinessPartnerRecord("SafeID2",Some(OrganisationResponse("Name")), EtmpAddress("Some ave",None,None,None,None, "GB")),
    SubscriberContact(name = None, Some("dave2"), Some("jones"), PhoneNumber("123456789").get,EmailAddress("bob@bob2.com")),
    CBCId("XHCBC0000000002"),
    Utr("7000000002")
  )

  val srb3 = SubscriptionDetails(
    BusinessPartnerRecord("SafeID3",Some(OrganisationResponse("Name")), EtmpAddress("Some ave",None,None,None,None, "GB")),
    SubscriberContact(name = None, Some("dave3"), Some("jones"), PhoneNumber("123456789").get,EmailAddress("bob@bob3.com")),
    CBCId("XHCBC0000000002"),
    Utr("9000000001")
  )


  "Attempt to regenerate all CBCIds generated during private beta" in {

    when(subDataRepo.getSubscriptions(any())) thenReturn Future.successful(List(srb1,srb2,srb3))
    when(local.createCBCId) thenReturn Future.successful(GenerateCBCIdResponse(CBCId.create(10)))
    when(subDataRepo.clear(any())) thenReturn Future.successful(DefaultWriteResult(true,1,Seq.empty,None,None,None))
    when(subDataRepo.save(any())) thenReturn Future.successful(DefaultWriteResult(true,1,Seq.empty,None,None,None))
    when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(200,None,Map.empty,None))
    when(auditMock.sendExtendedEvent(any())(any(),any())) thenReturn Future.successful(AuditResult.Success)

    new CBCIdRegenerationService(mockEmailConnector,subDataRepo,des,config ++ Configuration("CBCId.regenerate" -> true),local){
      override lazy val audit: Auditing = auditMock
    }

    eventually{verify(subDataRepo).getSubscriptions(any())}
    eventually{verify(local,times(3)).createCBCId}
    eventually{verify(subDataRepo, times(3)).clear(any())}
    eventually{verify(subDataRepo, times(3)).save(any())}
    eventually{verify(mockEmailConnector,times(3)).sendEmail(any())(any())}
    eventually{verify(auditMock, times(3)).sendExtendedEvent(any())(any(),any())}

  }

}
