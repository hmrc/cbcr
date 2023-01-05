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

package uk.gov.hmrc.cbcr.connectors

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{when, _}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.Email
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class EmailConnectorSpec extends UnitSpec with MockAuth with ScalaFutures with GuiceOneAppPerSuite {

  "send email" should {

    "submit request to email micro service to send email and get successful response status" in new Setup {

      // given
      when(httpMock.POST[Email, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(202, "202")))

      // when
      val result: Future[HttpResponse] = connector.sendEmail(correctEmail)
      await(result).status shouldBe 202

      // and
      val expectedResponseBody = Email(List("tyrion.lannister@gmail.com"), templateId, paramsSub)
      verify(httpMock).POST(any(), body = eqTo(expectedResponseBody), any())(any(), any(), any(), any())
    }
  }

  sealed trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
    val httpMock: HttpClient = mock[HttpClient]
    val templateId = "cbcr_subscription"
    val recipient = "user@example.com"
    val paramsSub = Map("f_name" → "Tyrion", "s_name" → "Lannister", "cbcrId" -> "XGCBC0000000001")
    val correctEmail: Email = Email(List("tyrion.lannister@gmail.com"), "cbcr_subscription", paramsSub)

    val config = app.injector.instanceOf[Configuration]

    val connector = new EmailConnectorImpl(config, httpMock)

  }

}
