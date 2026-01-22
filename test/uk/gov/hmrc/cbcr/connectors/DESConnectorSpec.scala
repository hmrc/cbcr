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
import play.api.http.Status.ACCEPTED
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.{Application, Configuration}
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.util.{UnitSpec, WireMockMethods}
import uk.gov.hmrc.cbcr.emailaddress.EmailAddress
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.test.WireMockSupport
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class DESConnectorSpec
    extends UnitSpec with MockAuth with ScalaFutures with GuiceOneAppPerSuite with WireMockMethods
    with WireMockSupport {

  implicit val hc: HeaderCarrier =
    HeaderCarrier().withExtraHeaders(("Environment", "MDTP_DEV"), ("Authorization", "Bearer "))

  val cd: CorrespondenceDetails = CorrespondenceDetails(
    EtmpAddress("line1", None, None, None, None, "GB"),
    ContactDetails(EmailAddress("test@test.com"), PhoneNumber("9876543").get),
    ContactName("FirstName", "Surname")
  )

  private val lookupData: JsObject = Json.obj(
    "regime"            -> "ITSA",
    "requiresNameMatch" -> false,
    "isAnAgent"         -> false
  )

  lazy val cbcSubscribeURI: String = "country-by-country/subscription"
  lazy val orgLookupURI: String = "registration/organisation"
  lazy val safeID: String = "safeID"

  private val config = Configuration(
    ConfigFactory.parseString(
      s"""
         |microservice {
         |  services {
         |      etmp-hod {
         |        host     = $wireMockHost
         |        port     = $wireMockPort
         |    }
         |  }
         |}
         |""".stripMargin
    )
  )

  override def fakeApplication(): Application = new GuiceApplicationBuilder().configure(config).build()

  "lookup" should {
    "submit request to lookup and get successful response status" in new Setup {
      val utr = "700000002"
      when(POST, s"/$orgLookupURI/utr/$utr", body = Some(Json.toJson(lookupData).toString()))
        .thenReturn(ACCEPTED)

      val result: Future[HttpResponse] = connector.lookup(utr)
      await(result).status shouldBe 202
    }

    "return an error when the future fails" in new Setup {
      val utr = "700000003"
      when(POST, s"/$orgLookupURI/utr/$utr", body = Some(Json.toJson(lookupData).toString()))
        .thenReturn(502)

      val result: Future[HttpResponse] = connector.lookup(utr)
      await(result).status shouldBe 502
    }
  }

  "customDesRead" should {
    "successfully convert 429 from DES to 503" in new Setup {
      val httpResponse: HttpResponse = HttpResponse(429, "429")
      val ex: UpstreamErrorResponse =
        intercept[UpstreamErrorResponse](connector.customDESRead("test", "testUrl", httpResponse))
      ex shouldBe UpstreamErrorResponse("429 received from DES - converted to 503", 429, 503)
    }
  }

  "createSubscription" should {
    "submit request to createSubscription and get successful response status" in new Setup {
      val sub: SubscriptionRequest = SubscriptionRequest("safeid", isMigrationRecord = false, cd)

      when(POST, s"/$cbcSubscribeURI", body = Some(Json.toJson(sub).toString()))
        .thenReturn(ACCEPTED)

      val result: Future[HttpResponse] = connector.createSubscription(sub)
      await(result).status shouldBe 202
    }
  }

  "updateSubscription" should {
    "submit request to updateSubscription and get successful response status" in new Setup {
      when(PUT, s"/$cbcSubscribeURI/$safeID", body = Some(CorrespondenceDetails.updateWriter.writes(cd).toString()))
        .thenReturn(ACCEPTED)

      val result: Future[HttpResponse] = connector.updateSubscription("safeID", cd)
      await(result).status shouldBe 202
    }
  }

  sealed trait Setup {
    implicit val hc: HeaderCarrier =
      HeaderCarrier().withExtraHeaders(("Environment", "MDTP_DEV"), ("Authorization", "Bearer "))
    implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
    val mockAuditConnector: AuditConnector = mock[AuditConnector]
    val servicesConfig: ServicesConfig = mock[ServicesConfig]
    val connector: DESConnectorImpl = app.injector.instanceOf[DESConnectorImpl]
  }
}
