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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import cats.data.OptionT
import cats.data.Validated.Invalid
import cats.syntax.option._
import cats.instances.future._
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import CBCIdGenCommands.{GenerateCBCId, GenerateCBCIdResponse}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class LocalSubscriptionSpec  extends TestKit(ActorSystem("CBCIdControllerSpec",ConfigFactory.parseString("""
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
""".stripMargin))) with UnitSpec with Matchers with ScalaFutures with OneAppPerSuite with MockitoSugar{

  implicit val mat = ActorMaterializer()

  val testCBCIdGenerator = TestProbe("testCBCID")
  val config = system.settings.config
  implicit val as = app.injector.instanceOf[ActorSystem]
  implicit val ec = app.injector.instanceOf[ExecutionContext]
  val repo = mock[SubscriptionDataRepository]

  val localGen = new LocalSubscription(Configuration(config), repo){
    override private[services] lazy val cbcIdGenerator = testCBCIdGenerator.ref
  }

  val bpr = BusinessPartnerRecord("MySafeID",Some(OrganisationResponse("Dave Corp")),EtmpAddress("13 Accacia Ave",None,None,None,None,"GB"))
  val exampleSubscriptionData = SubscriptionDetails(bpr,SubscriberContact(name = None, Some("Dave"), Some("Jones"),PhoneNumber("02072653787").get,EmailAddress("dave@dave.com")),CBCId("XGCBC0000000001"),Utr("utr"))

  val cd = CorrespondenceDetails(
      EtmpAddress("line1",None,None,None,None,"GB"),
      ContactDetails(EmailAddress("test@test.com"),PhoneNumber("9876543").get),
      ContactName("FirstName","Surname")
  )

  val sub = SubscriptionRequest("safeid",false, cd)

  implicit val hc = HeaderCarrier()


  "The LocalCBCIdGenerator" should {
    "be able to create a new subscription and" when {
      "everything works, respond with a 200" in {
        val response = localGen.createSubscription(sub)
        testCBCIdGenerator.expectMsg(GenerateCBCId)
        testCBCIdGenerator.reply(GenerateCBCIdResponse(CBCId("XGCBC0000000001").toValid(new Exception("Test Error generating CBCId"))))
        status(response) shouldBe Status.OK
        jsonBodyOf(response).futureValue shouldEqual Json.obj("cbc-id" -> "XGCBC0000000001")
      }
      "the CBCIdGenerator service fails, respond with a 500" in {
        val response = localGen.createSubscription(sub)
        testCBCIdGenerator.expectMsg(GenerateCBCId)
        testCBCIdGenerator.reply(GenerateCBCIdResponse(Invalid(new Exception("Failed to generate CBCId"))))
        status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "the CBCIdGenerator service fails to respond, respond with a 500" in {
        val response = localGen.createSubscription(sub)
        testCBCIdGenerator.expectMsg(GenerateCBCId)
        status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
    "be able to update a subscription" which {
      "responds with a 200 as it doesn't really do anything" in {
        val response = localGen.updateSubscription("safeID",cd)
        status(response) shouldBe Status.OK
      }
    }
    "be able to display subscription information" which {
      "queries our own subscription store and converts to the correct format" in {
        when(repo.get(exampleSubscriptionData.businessPartnerRecord.safeId)) thenReturn OptionT.pure[Future,SubscriptionDetails](exampleSubscriptionData)
        val response = localGen.getSubscription(exampleSubscriptionData.businessPartnerRecord.safeId)
        status(response) shouldBe Status.OK

        val jResponse = GetResponse(
          bpr.safeId,
          ContactName(exampleSubscriptionData.subscriberContact.firstName.getOrElse(""),exampleSubscriptionData.subscriberContact.lastName.getOrElse("")),
          ContactDetails(exampleSubscriptionData.subscriberContact.email,exampleSubscriptionData.subscriberContact.phoneNumber),
          exampleSubscriptionData.businessPartnerRecord.address
        )

        jsonBodyOf(response).futureValue shouldEqual Json.toJson(jResponse)

      }
    }
  }

}
