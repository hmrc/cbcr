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

package uk.gov.hmrc.cbcr.controllers

import org.apache.pekko.actor.ActorSystem
import com.mongodb.client.result.{DeleteResult, InsertOneResult}
import org.bson.BsonNull
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.cbcr.util.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionDataControllerSpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite {

  private val store = mock[SubscriptionDataRepository]

  private val config = app.injector.instanceOf[Configuration]

  private val bpr = BusinessPartnerRecord(
    "MySafeID",
    Some(OrganisationResponse("Dave Corp")),
    EtmpAddress("13 Accacia Ave", None, None, None, None, "GB")
  )
  private val exampleSubscriptionData = SubscriptionDetails(
    bpr,
    SubscriberContact(name = None, "Dave", "Jones", PhoneNumber("02072653787").get, EmailAddress("dave@dave.com")),
    CBCId("XGCBC0000000001"),
    Utr("utr")
  )
  private val exampleSubscriberContact =
    SubscriberContact(None, "firstName", "lastName", PhoneNumber("02072653787").get, EmailAddress("dave@dave.com"))

  private val desConnector = mock[DESConnector]
  when(store.getSubscriptions(any())).thenReturn(Future.successful(List()))
  val controller = new SubscriptionDataController(store, desConnector, cBCRAuth, config, cc)

  private val fakePostRequest: FakeRequest[JsValue] =
    FakeRequest(Helpers.POST, "/saveSubscriptionData").withBody(toJson(exampleSubscriptionData))

  private val BadFakePostRequest: FakeRequest[JsValue] =
    FakeRequest(Helpers.POST, "/saveSubscriptionData").withBody(Json.obj("bad" -> "request"))

  private val fakePutRequest =
    FakeRequest(Helpers.PUT, "/updateSubscriberContactDetails").withBody(toJson(exampleSubscriberContact))

  private val badFakePutRequest =
    FakeRequest(Helpers.PUT, "/updateSubscriberContactDetails").withBody(Json.obj("bad" -> "request"))

  private val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(Helpers.GET, "/retrieveSubscriptionData")

  private val fakeDeleteRequest = FakeRequest(Helpers.DELETE, "subscription-data")

  private implicit val as: ActorSystem = ActorSystem()

  private val cbcId = CBCId.create(1).getOrElse(fail("Couldn't generate cbcid"))
  private val utr = Utr("7000000002")

  "The SubscriptionDataController" should {
    "respond with a 200 when asked to store SubscriptionData" in {
      when(store.save2(any(classOf[SubscriptionDetails])))
        .thenReturn(Future.successful(InsertOneResult.acknowledged(BsonNull.VALUE)))
      val result = controller.saveSubscriptionData()(fakePostRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 400 if invalid SubscriptionData passed in request" in {
      val result = controller.saveSubscriptionData()(BadFakePostRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "respond with a 200 when asked to update SubscriptionData" in {
      when(store.update(any(), any(classOf[SubscriberContact]))) thenReturn Future.successful(true)
      val result = controller.updateSubscriberContactDetails(cbcId)(fakePutRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 500 if there is a DB failure during update" in {
      when(store.update(any(), any(classOf[SubscriberContact]))) thenReturn Future.successful(false)
      val result = controller.updateSubscriberContactDetails(cbcId)(fakePutRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "respond with a 400 if invalid SubscriberContact passed in request" in {
      val result = controller.updateSubscriberContactDetails(cbcId)(badFakePutRequest)
      status(result) shouldBe Status.BAD_REQUEST
    }

    "respond with a 200 and a SubscriptionData when asked to retrieve an existing CBCID" in {
      when(store.get(any(classOf[CBCId])))
        .thenReturn(Future(Some(exampleSubscriptionData)))
      val result = controller.retrieveSubscriptionDataCBCId(cbcId)(fakeGetRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(result).validate[SubscriptionDetails].isSuccess shouldBe true
    }

    "respond with a 404 when asked to retrieve a non-existent CBCID" in {
      when(store.get(any(classOf[CBCId]))).thenReturn(Future(None))
      val result = controller.retrieveSubscriptionDataCBCId(cbcId)(fakeGetRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "respond with a 200 when queried with a utr that already exists" in {
      when(store.get(any(classOf[Utr])))
        .thenReturn(Future(Some(exampleSubscriptionData)))
      val result = controller.retrieveSubscriptionDataUtr(utr)(fakeGetRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 404 when queried with a utr that doesnt exist" in {
      when(store.get(any(classOf[Utr]))).thenReturn(Future(None))
      val result = controller.retrieveSubscriptionDataUtr(utr)(fakeGetRequest)
      status(result) shouldBe Status.NOT_FOUND

    }

    "respond with a 200 when asked to clear a record that exists" in {
      when(store.clearCBCId(any(classOf[CBCId])))
        .thenReturn(Future(DeleteResult.acknowledged(1L)))
      val result = controller.clearSubscriptionData(cbcId)(fakeDeleteRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 404 when asked to clear a record that doesn't exist" in {
      when(store.clearCBCId(any(classOf[CBCId])))
        .thenReturn(Future(DeleteResult.acknowledged(0L)))
      val result = controller.clearSubscriptionData(cbcId)(fakeDeleteRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

  }
}
