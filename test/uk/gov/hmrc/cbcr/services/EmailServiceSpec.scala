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
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.cbcr.connectors.EmailConnectorImpl
import uk.gov.hmrc.cbcr.models.Email
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import play.api.mvc.Results._
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import scala.concurrent.ExecutionContext.Implicits.global


import scala.concurrent

class EmailServiceSpec extends UnitSpec with MockitoSugar with OneAppPerSuite with ScalaFutures {

  val mockEmailConnector = mock[EmailConnectorImpl]
  val mockAuditConnector = mock[AuditConnectorI]
  val emailService = new EmailService(mockEmailConnector, mockAuditConnector)
  val paramsSub = Map("f_name" → "Tyrion","s_name" → "Lannister", "cbcrId" -> "XGCBC0000000001")
  val correctEmail: Email = Email(List("tyrion.lannister@gmail.com"), "cbcr_subscription", paramsSub)
  implicit val hc = HeaderCarrier()

  "the email service" should {
    "return 202 when everything is ok" in {

      when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(202))
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(Success)
      val result: Future[Result] = emailService.sendEmail(correctEmail)
      await(result) shouldBe Accepted
    }

    "return 400 when everything is ok" in {

      when(mockEmailConnector.sendEmail(any())(any())) thenReturn Future.successful(HttpResponse(400))
      when(mockAuditConnector.sendExtendedEvent(any())(any(), any())) thenReturn Future.successful(Success)
      val result: Future[Result] = emailService.sendEmail(correctEmail)
      await(result) shouldBe BadRequest
    }
  }

}
