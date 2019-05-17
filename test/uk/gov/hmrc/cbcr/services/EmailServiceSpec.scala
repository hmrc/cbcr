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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.cbcr.connectors.EmailConnectorImpl
import uk.gov.hmrc.cbcr.models.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Success,Disabled,Failure}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class EmailServiceSpec extends UnitSpec with MockitoSugar with OneAppPerSuite with ScalaFutures {

  val mockEmailConnector = mock[EmailConnectorImpl]
  val mockAuditConnector = mock[AuditConnectorI]
  val runMode = mock[RunMode]
  val config = app.injector.instanceOf[Configuration]


  val emailService = new EmailService(mockEmailConnector, mockAuditConnector, config, runMode)
  val paramsSub = Map("f_name" → "Tyrion","s_name" → "Lannister", "cbcrId" -> "XGCBC0000000001")
  val correctEmail: Email = Email(List("tyrion.lannister@gmail.com"), "cbcr_subscription", paramsSub)
  implicit val hc = HeaderCarrier()

  "the email service" should {
    "return 202 when everything is ok" in {

      when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(202))
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(Success)
      when(runMode.env) thenReturn "Dev"
      val result: Future[Result] = emailService.sendEmail(correctEmail)
      await(result) shouldBe Accepted
    }

    "return 400 when everything is ok" in {

      when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(400))
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(Success)
      when(runMode.env) thenReturn "Dev"

      val result: Future[Result] = emailService.sendEmail(correctEmail)
      await(result) shouldBe BadRequest
    }

    "return 400 when everything is ok but audit fails" in {

      when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(400))
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(Failure("test to designed to provoke an emotional response",None))
      when(runMode.env) thenReturn "Dev"

      val result: Future[Result] = emailService.sendEmail(correctEmail)
      await(result) shouldBe BadRequest
    }

    "return 400 when everything is ok but audit disabled" in {

      when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(400))
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(Disabled)
      when(runMode.env) thenReturn "Dev"

      val result: Future[Result] = emailService.sendEmail(correctEmail)
      await(result) shouldBe BadRequest
    }
  }

}
