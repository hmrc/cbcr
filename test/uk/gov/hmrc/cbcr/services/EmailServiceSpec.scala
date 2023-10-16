/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.cbcr.connectors.EmailConnectorImpl
import uk.gov.hmrc.cbcr.models.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.cbcr.util.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailServiceSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite with ScalaFutures {

  val mockEmailConnector = mock[EmailConnectorImpl]
  val mockAuditConnector = mock[AuditConnector]
  val config = app.injector.instanceOf[Configuration]

  val emailService = new EmailService(mockEmailConnector, mockAuditConnector, config)
  val paramsSub = Map("f_name" -> "Tyrion", "s_name" -> "Lannister", "cbcrId" -> "XGCBC0000000001")
  val correctEmail: Email = Email(List("tyrion.lannister@gmail.com"), "cbcr_subscription", paramsSub)
  implicit val hc = HeaderCarrier()

  "the email service" should {
    "return 202 when everything is ok" in {

      when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(202, "202"))
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(Success)
      val result: Future[Result] = emailService.sendEmail(correctEmail)
      await(result) shouldBe Accepted
    }

    "return 400 when everything is ok" in {

      when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(400, "400"))
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(Success)

      val result: Future[Result] = emailService.sendEmail(correctEmail)
      await(result) shouldBe BadRequest
    }

    "return 400 when everything is ok but audit fails" in {

      when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(400, "400"))
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(
        Failure("test to designed to provoke an emotional response", None))

      val result: Future[Result] = emailService.sendEmail(correctEmail)
      await(result) shouldBe BadRequest
    }

    "return 400 when everything is ok but audit disabled" in {

      when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(400, "400"))
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(Disabled)

      val result: Future[Result] = emailService.sendEmail(correctEmail)
      await(result) shouldBe BadRequest
    }
  }

}
