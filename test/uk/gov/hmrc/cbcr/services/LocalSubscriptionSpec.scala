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

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class LocalSubscriptionSpec
    extends TestKit(
      ActorSystem(
        "CBCIdControllerSpec",
        ConfigFactory.parseString("""
                                    |akka {
                                    | persistence {
                                    |   journal.plugin = "inmemory-journal"
                                    |   snapshot-store.plugin = "inmemory-snapshot-store"
                                    | }
                                    |}
                                    |CBCId.controller {
                                    |  timeout = 2 seconds
                                    |  supervisor {
                                    |    minBackoff = 3 seconds
                                    |    maxBackoff = 10 minutes
                                    |  }
                                    |}
""".stripMargin)
      )) with UnitSpec with Matchers with ScalaFutures with GuiceOneAppPerSuite with MockitoSugar {

  private implicit val as: ActorSystem = app.injector.instanceOf[ActorSystem]
  private implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  private val repo = mock[SubscriptionDataRepository]
  private val cbcdIdGenerator = new CBCIdGenerator

  private val localGen = new LocalSubscription(repo, cbcdIdGenerator)

  private val bpr = BusinessPartnerRecord(
    "MySafeID",
    Some(OrganisationResponse("Dave Corp")),
    EtmpAddress("13 Accacia Ave", None, None, None, None, "GB"))
  private val exampleSubscriptionData = SubscriptionDetails(
    bpr,
    SubscriberContact(name = None, "Dave", "Jones", PhoneNumber("02072653787").get, EmailAddress("dave@dave.com")),
    CBCId("XGCBC0000000001"),
    Utr("utr"))

  private val cd = CorrespondenceDetails(
    EtmpAddress("line1", None, None, None, None, "GB"),
    ContactDetails(EmailAddress("test@test.com"), PhoneNumber("9876543").get),
    ContactName("FirstName", "Surname")
  )

  private val sub = SubscriptionRequest("safeid", isMigrationRecord = false, cd)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "The LocalCBCIdGenerator" should {
    "be able to create a new subscription and" when {
      "everything works, respond with a 200" in {
        val response = localGen.createSubscription(sub)
        status(response) shouldBe Status.OK
      }
    }
    "be able to update a subscription" which {
      "responds with a 200 as it doesn't really do anything" in {
        val response = localGen.updateSubscription("safeID", cd)
        status(response) shouldBe Status.OK
      }
    }
    "be able to display subscription information" which {
      "queries our own subscription store and converts to the correct format" in {
        when(repo.get(exampleSubscriptionData.businessPartnerRecord.safeId)) thenReturn
          Future(Some(exampleSubscriptionData))
        val response = localGen.getSubscription(exampleSubscriptionData.businessPartnerRecord.safeId)
        status(response) shouldBe Status.OK

        val jResponse = GetResponse(
          bpr.safeId,
          ContactName(
            exampleSubscriptionData.subscriberContact.firstName,
            exampleSubscriptionData.subscriberContact.lastName),
          ContactDetails(
            exampleSubscriptionData.subscriberContact.email,
            exampleSubscriptionData.subscriberContact.phoneNumber),
          exampleSubscriptionData.businessPartnerRecord.address
        )

        jsonBodyOf(response).futureValue shouldEqual Json.toJson(jResponse)

      }

      "queries our own subscription store but finds nothing" in {
        when(repo.get(exampleSubscriptionData.businessPartnerRecord.safeId)) thenReturn Future(None)
        val response = localGen.getSubscription(exampleSubscriptionData.businessPartnerRecord.safeId)
        status(response) shouldBe Status.NOT_FOUND

      }
    }
  }

}
