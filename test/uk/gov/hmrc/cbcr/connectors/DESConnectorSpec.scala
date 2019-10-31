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

package uk.gov.hmrc.cbcr.connectors

import com.typesafe.config.Config
import org.mockito.ArgumentMatchers.{eq => EQ}
import uk.gov.hmrc.cbcr.controllers.MockAuth
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.cbcr.services.RunMode
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.cbcr.models.{BusinessPartnerRecord, CBCId, ContactDetails, ContactName, CorrespondenceDetails, Email, EtmpAddress, OrganisationResponse, PhoneNumber, SubscriberContact, SubscriptionDetails, SubscriptionRequest, Utr}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class DESConnectorSpec extends UnitSpec with MockAuth with ScalaFutures with OneAppPerSuite {

  "lookup" should {

    "submit request to lookup and get successful response status" in new Setup {
      val utr = "700000002"
      when(httpMock.POST[JsValue, HttpResponse]
        (
          any(),
          any(),
          any())
        (any(), any(), any(), any())
      ).thenReturn(Future.successful(HttpResponse(202)))
      val result: Future[HttpResponse] = connector.lookup(utr)
      await(result).status shouldBe 202
    }
  }

    "createSubscription" should {

      "submit request to createSubscription and get successful response status" in new Setup {
        val sub = SubscriptionRequest("safeid", false, cd)
        when(httpMock.POST[SubscriptionRequest, HttpResponse]
          (
            any(),
            any(),
            any())
          (any(), any(), any(), any())
        ).thenReturn(Future.successful(HttpResponse(202)))
        val result: Future[HttpResponse] = connector.createSubscription(sub)
        await(result).status shouldBe 202
      }
    }
      "updateSubscription" should {

        "submit request to updateSubscription and get successful response status" in new Setup {
          when(httpMock.PUT[CorrespondenceDetails, HttpResponse]
            (
              any(),
              any(),
              any())
            (any(), any(), any(), any())
          ).thenReturn(Future.successful(HttpResponse(202)))
          val result: Future[HttpResponse] = connector.updateSubscription("safeID", cd)
          await(result).status shouldBe 202
        }
      }
        /*"getSubscription" should {

          "submit request to getSubscription and get successful response status" in new Setup {
            val bpr = BusinessPartnerRecord("MySafeID",Some(OrganisationResponse("Dave Corp")),EtmpAddress("13 Accacia Ave",None,None,None,None,"GB"))
            val exampleSubscriptionData = SubscriptionDetails(bpr,SubscriberContact(name = None, "Dave", "Jones",PhoneNumber("02072653787").get,EmailAddress("dave@dave.com")),CBCId("XGCBC0000000001"),Utr("utr"))
            when(httpMock.GET[HttpResponse]
              (
                any(),
                Seq(any()),
                Seq(any()))
              (any(), any(), any())
            ).thenReturn(Future.successful(HttpResponse(202)))
            val result: Future[HttpResponse] = connector.getSubscription("safeId" )
            await(result).status shouldBe 202
          }
  }*/

  sealed trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val executionContext: ExecutionContextExecutor = ExecutionContext.Implicits.global
    val mockAuditConnector = mock[AuditConnector]
    val runMode = mock[RunMode]
    val httpMock: HttpClient = mock[HttpClient]
    val servicesConfig=mock[ServicesConfig]

    val config = app.injector.instanceOf[Configuration]

    val cd = CorrespondenceDetails(
      EtmpAddress("line1",None,None,None,None,"GB"),
      ContactDetails(EmailAddress("test@test.com"),PhoneNumber("9876543").get),
      ContactName("FirstName","Surname")
    )


    val connector = new DESConnectorImpl(executionContext, mockAuditConnector,config,runMode,httpMock,servicesConfig)

  }



}
