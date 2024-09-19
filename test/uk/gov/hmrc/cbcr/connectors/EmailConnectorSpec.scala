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

import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Configuration}
import play.api.http.Status.ACCEPTED
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.Email
import uk.gov.hmrc.cbcr.util.{UnitSpec, WireMockMethods}
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext

class EmailConnectorSpec
    extends UnitSpec with MockAuth with ScalaFutures with GuiceOneAppPerSuite with WireMockMethods
    with WireMockSupport {

  private val config = Configuration(
    ConfigFactory.parseString(
      s"""
         |microservice {
         |  services {
         |      email {
         |        protocol = http
         |        host     = $wireMockHost
         |        port     = $wireMockPort
         |    }
         |  }
         |}
         |""".stripMargin
    )
  )

  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure(config).build()

  "send email" should {

    "submit request to email micro service to send email and get successful response status" in new Setup {

      val expectedResponseBody: Email = Email(List("tyrion.lannister@gmail.com"), templateId, paramsSub)

      // given
      when(POST, "/hmrc/email/", body = Some(Json.toJson(correctEmail).toString()))
        .thenReturn(ACCEPTED, expectedResponseBody)

      // when
      val result: HttpResponse = await(connector.sendEmail(correctEmail))
      result.status shouldBe 202
      Json.fromJson[Email](result.json).get shouldBe expectedResponseBody

    }
  }

  sealed trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
    val templateId = "cbcr_subscription"
    val recipient = "user@example.com"
    val paramsSub: Map[String, String] =
      Map("f_name" -> "Tyrion", "s_name" -> "Lannister", "cbcrId" -> "XGCBC0000000001")
    val correctEmail: Email = Email(List("tyrion.lannister@gmail.com"), "cbcr_subscription", paramsSub)

    val connector: EmailConnectorImpl = app.injector.instanceOf[EmailConnectorImpl]

  }
}
